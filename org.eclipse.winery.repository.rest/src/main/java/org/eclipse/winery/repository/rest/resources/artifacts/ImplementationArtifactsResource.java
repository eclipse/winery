/*******************************************************************************
 * Copyright (c) 2012-2018 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.artifacts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.tosca.TImplementationArtifact;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.INodeTypeImplementationResourceOrRelationshipTypeImplementationResource;
import org.eclipse.winery.repository.rest.resources.apiData.InterfacesSelectApiData;
import org.eclipse.winery.repository.rest.resources.entitytypeimplementations.nodetypeimplementations.NodeTypeImplementationResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ImplementationArtifact instead of TImplementationArtifact has to be used because of difference in the XSD at
 * tImplementationArtifacts vs. tDeploymentArtifacts
 */
public class ImplementationArtifactsResource extends GenericArtifactsResource<ImplementationArtifactResource, TImplementationArtifact> {

    private final static Logger logger = LoggerFactory.getLogger(ImplementationArtifactsResource.class);

    private List<TImplementationArtifact> implementationArtifacts;

    public ImplementationArtifactsResource(List<TImplementationArtifact> implementationArtifact, INodeTypeImplementationResourceOrRelationshipTypeImplementationResource res) {
        super(ImplementationArtifactResource.class, TImplementationArtifact.class, implementationArtifact, res);
        this.implementationArtifacts = implementationArtifact;
    }

    @Override
    public Collection<ImplementationArtifactResource> getAllArtifactResources() {
        Collection<ImplementationArtifactResource> res = new ArrayList<>(this.implementationArtifacts.size());
        for (TImplementationArtifact da : this.implementationArtifacts) {
            ImplementationArtifactResource r = new ImplementationArtifactResource(da, this.implementationArtifacts, this.res);
            res.add(r);
        }
        return res;
    }

    /**
     * Method to get all interfaces associated to a nodetype or relationshiptype
     *
     * @return a list of TInterface
     */
    @Path("interfaces/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<?> getInterfacesOfAssociatedType() {
        // TODO refactor this that IRepository offers this helper method

        boolean isNodeTypeImplementation = this.res instanceof NodeTypeImplementationResource;
        QName type = RestUtils.getType(this.res);
        List<InterfacesSelectApiData> interfaces = new ArrayList<>();
        if (isNodeTypeImplementation) {
            TNodeType nodeType = RepositoryFactory.getRepository().getElement(new NodeTypeId(type));
            boolean typeToHandle;

            if (nodeType == null) {
                logger.error("Repository corrupt! Relationship Type '{}' cannot be found!", type);
            } else {
                do {
                    mergeInterfaces(interfaces, nodeType.getInterfaces());

                    if (nodeType.getDerivedFrom() != null) {
                        QName parentType = nodeType.getDerivedFrom().getType();
                        nodeType = RepositoryFactory.getRepository().getElement(new NodeTypeId(parentType));
                        typeToHandle = true;
                    } else {
                        typeToHandle = false;
                    }
                } while (typeToHandle);
            }
        } else {
            TRelationshipType relationshipType = RepositoryFactory.getRepository().getElement(new RelationshipTypeId(type));
            boolean typeToHandle;

            if (relationshipType == null) {
                logger.error("Repository corrupt! Relationship Type '{}' cannot be found!", type);
            } else {
                do {
                    mergeInterfaces(interfaces, relationshipType.getInterfaces());
                    mergeInterfaces(interfaces, relationshipType.getSourceInterfaces());
                    mergeInterfaces(interfaces, relationshipType.getTargetInterfaces());

                    if (relationshipType.getDerivedFrom() != null) {
                        QName parentType = relationshipType.getDerivedFrom().getTypeAsQName();
                        relationshipType = RepositoryFactory.getRepository().getElement(new RelationshipTypeId(parentType));
                        typeToHandle = true;
                    } else {
                        typeToHandle = false;
                    }
                } while (typeToHandle);
            }
        }
        return interfaces;
    }

    private void mergeInterfaces(List<InterfacesSelectApiData> interfaces, List<TInterface> interfacesList) {
        if (interfacesList != null) {
            for (TInterface notContainedInterface : interfacesList) {
                Optional<InterfacesSelectApiData> foundInterface = interfaces.stream()
                    .filter(containedInterface -> containedInterface.getId().equals(notContainedInterface.getName()))
                    .findFirst();

                if (foundInterface.isPresent()) {
                    InterfacesSelectApiData apiDateInterface = foundInterface.get();
                    List<TOperation> notContainedOperations = notContainedInterface.getOperations().stream()
                        .filter(operation -> !apiDateInterface.operations.contains(operation.getName()))
                        .collect(Collectors.toList());

                    if (!notContainedOperations.isEmpty()) {
                        apiDateInterface.operations.addAll(notContainedOperations.stream()
                            .map(TOperation::getName)
                            .collect(Collectors.toList()));
                    }
                } else {
                    interfaces.add(RestUtils.convertInterfaceToSelectApiData(notContainedInterface));
                }
            }
        }
    }

    @Override
    public String getId(TImplementationArtifact entity) {
        return entity.getName();
    }

    @Override
    @Path("{id}/")
    public ImplementationArtifactResource getEntityResource(@PathParam("id") String id) {
        return this.getEntityResourceFromEncodedId(id);
    }
}

