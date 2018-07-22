/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.repository.substitution;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Substitution {

    private static final Logger LOGGER = LoggerFactory.getLogger(Substitution.class);
    private static final String versionAppendix = "substituted";

    private final IRepository repository;

    private final Map<QName, TServiceTemplate> nodeTypeSubstitutableWithServiceTemplate = new HashMap<>();
    private final Map<QName, TRelationshipType> relationshipTypes = new HashMap<>();
    private final Map<QName, TNodeType> nodeTypes = new HashMap<>();

    public Substitution() {
        repository = RepositoryFactory.getRepository();
    }

    public ServiceTemplateId substituteTopology(final ServiceTemplateId serviceTemplateId) {
        // 0. Create a new version of the Service Template
        ServiceTemplateId substitutedServiceTemplateId = getSubstitutionServiceTemplateId(serviceTemplateId);
        TServiceTemplate serviceTemplate = repository.getElement(substitutedServiceTemplateId);
        TTopologyTemplate topology = Objects.requireNonNull(serviceTemplate.getTopologyTemplate());

        loadAllRequiredDefinitionsForTopologySubstitution();

        // 1. Step: retrieve all Node Templates which must be substituted
        Map<TNodeTemplate, List<Subtypes<TNodeType>>> substitutableNodeTemplates =
            SubstitutionUtils.collectSubstitutableTemplates(topology.getNodeTemplates(), this.nodeTypes);

        // 2. Step: retrieve all Relationship Templates which must be substituted
        Map<TRelationshipTemplate, List<Subtypes<TRelationshipType>>> substitutableRelationshipTemplates =
            SubstitutionUtils.collectSubstitutableTemplates(topology.getRelationshipTemplates(), this.relationshipTypes);

        // 3. Step: select concrete type to be substituted
        Map<TNodeTemplate, TNodeType> nodeTemplateReplacementMap =
            new FindFirstSubstitutionStrategy<TNodeTemplate, TNodeType>()
                .getReplacementMap(substitutableNodeTemplates);
        Map<TRelationshipTemplate, TRelationshipType> relationshipTemplateReplacementMap =
            new FindFirstSubstitutionStrategy<TRelationshipTemplate, TRelationshipType>()
                .getReplacementMap(substitutableRelationshipTemplates);

        // 4. Step: update the topology
        updateTopology(topology, nodeTemplateReplacementMap, relationshipTemplateReplacementMap);

        try {
            BackendUtils.persist(repository, substitutedServiceTemplateId, serviceTemplate);
        } catch (IOException e) {
            LOGGER.debug("Could not persist Service Template", e);
        }

        return substitutedServiceTemplateId;
    }

    private void updateTopology(TTopologyTemplate topologyTemplate, Map<TNodeTemplate, TNodeType> nodeTemplateReplacementMap,
                                Map<TRelationshipTemplate, TRelationshipType> relationshipTemplateReplacementMap) {
        Map<TNodeTemplate, TServiceTemplate> nodeTemplateToBeSubstitutedWithTopology = new HashMap<>();

        topologyTemplate.getNodeTemplates()
            .forEach(tNodeTemplate -> {
                TServiceTemplate serviceTemplate = this.nodeTypeSubstitutableWithServiceTemplate.get(tNodeTemplate.getType());
                if (Objects.nonNull(serviceTemplate)) {
                    // We need to replace the Node Template with the serviceTemplate but we cannot do that here
                    // -> save it for later processing
                    nodeTemplateToBeSubstitutedWithTopology.put(tNodeTemplate, serviceTemplate);
                } else {
                    // In case of simple NodeTemplate substitution
                    // -> everything is inherited -> there is no need to change anything else
                    TNodeType replacementType = nodeTemplateReplacementMap.get(tNodeTemplate);
                    if (Objects.nonNull(replacementType)) {
                        QName qName = new QName(replacementType.getTargetNamespace(), replacementType.getIdFromIdOrNameField());
                        tNodeTemplate.setType(qName);
                    }
                }
            });

        topologyTemplate.getRelationshipTemplates()
            .forEach(tRelationshipTemplate -> {
                TRelationshipType tRelationshipType = relationshipTemplateReplacementMap.get(tRelationshipTemplate);
                if (Objects.nonNull(tRelationshipType)) {
                    QName qName = new QName(tRelationshipType.getTargetNamespace(), tRelationshipType.getIdFromIdOrNameField());
                    tRelationshipTemplate.setType(qName);
                }
            });

        replaceNodeTemplateWithServiceTemplate(topologyTemplate, nodeTemplateToBeSubstitutedWithTopology);
    }

    private void replaceNodeTemplateWithServiceTemplate(TTopologyTemplate topologyTemplate, Map<TNodeTemplate, TServiceTemplate> nodeTemplateToBeSubstitutedWithTopology) {
        nodeTemplateToBeSubstitutedWithTopology.forEach((tNodeTemplate, tServiceTemplate) -> {
            // 1. get all references of the Node Template
            // 2. import the topology in the Service Template
            // 3. update the references accordingly
        });
    }

    /**
     * Creates a new version of the given Service Template for the substitution.
     * If no new version can be created, the given Service Template will be returned.
     *
     * @param serviceTemplateId the Service Template containing abstract types to be substituted
     * @return the new Id of the Service Template
     */
    private ServiceTemplateId getSubstitutionServiceTemplateId(ServiceTemplateId serviceTemplateId) {
        try {
            ServiceTemplateId substitutedServiceTemplateId = new ServiceTemplateId(
                serviceTemplateId.getNamespace().getDecoded(),
                VersionUtils.getNewId(serviceTemplateId, versionAppendix),
                false
            );

            repository.duplicate(serviceTemplateId, substitutedServiceTemplateId);
            LOGGER.debug("Created new Service Template version {}", substitutedServiceTemplateId);

            return substitutedServiceTemplateId;
        } catch (IOException e) {
            LOGGER.debug("Could not create new Service Template version during substitution", e);
            LOGGER.debug("Reusing existing element");
        }

        return serviceTemplateId;
    }

    private void loadAllRequiredDefinitionsForTopologySubstitution() {
        this.repository.getAllDefinitionsChildIds(ServiceTemplateId.class)
            .stream()
            .map(repository::getElement)
            .filter(element -> Objects.nonNull(element.getSubstitutableNodeType()))
            .forEach(tServiceTemplate ->
                this.nodeTypeSubstitutableWithServiceTemplate.put(tServiceTemplate.getSubstitutableNodeType(), tServiceTemplate)
            );

        this.repository.getAllDefinitionsChildIds(NodeTypeId.class)
            .forEach(id ->
                this.nodeTypes.put(id.getQName(), this.repository.getElement(id))
            );

        this.repository.getAllDefinitionsChildIds(RelationshipTypeId.class)
            .forEach(id ->
                this.relationshipTypes.put(id.getQName(), this.repository.getElement(id))
            );
    }
}