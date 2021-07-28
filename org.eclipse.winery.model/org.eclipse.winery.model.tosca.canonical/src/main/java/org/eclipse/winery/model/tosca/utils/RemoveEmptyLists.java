/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.utils;

import java.util.List;

import org.eclipse.winery.model.tosca.TArtifact;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TPolicies;
import org.eclipse.winery.model.tosca.TPropertyConstraint;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.visitor.Visitor;

import io.github.adr.embedded.ADR;

/**
 * This class removes empty lists. For instance, if a node template has a list of policies and this list is empty, it is
 * removed. This is important for XSD validation.
 *
 * Use it by calling <code>removeEmptyLists(topologyTemplate)</code>
 *
 * TODO: Extend this for all model elements
 */
@ADR(22)
public class RemoveEmptyLists extends Visitor {

    @Override
    public void visit(TEntityTemplate entityTemplate) {
        final List<TPropertyConstraint> propertyConstraints = entityTemplate.getPropertyConstraints();
        if ((propertyConstraints != null) && propertyConstraints.isEmpty()) {
            entityTemplate.setPropertyConstraints(null);
        }
        TEntityTemplate.Properties properties = entityTemplate.getProperties();
        if (properties instanceof TEntityTemplate.XmlProperties) {
            if (((TEntityTemplate.XmlProperties) properties).getAny() == null) {
                entityTemplate.setProperties(null);
            }
        }
        if (properties instanceof TEntityTemplate.WineryKVProperties) {
            if (((TEntityTemplate.WineryKVProperties) properties).getKVProperties().isEmpty()) {
                entityTemplate.setProperties(null);
            }
        }
        if (properties instanceof TEntityTemplate.YamlProperties) {
            if (((TEntityTemplate.YamlProperties) properties).getProperties().isEmpty()) {
                entityTemplate.setProperties(null);
            }
        }
        super.visit(entityTemplate);
    }

    @Override
    public void visit(TNodeTemplate nodeTemplate) {
        final List<TRequirement> requirements = nodeTemplate.getRequirements();
        if ((requirements != null) && requirements.isEmpty()) {
            nodeTemplate.setRequirements(null);
        }
        final List<TCapability> capabilities = nodeTemplate.getCapabilities();
        if ((capabilities != null) && capabilities.isEmpty()) {
            nodeTemplate.setCapabilities(null);
        }
        final List<TDeploymentArtifact> deploymentArtifacts = nodeTemplate.getDeploymentArtifacts();
        if (deploymentArtifacts != null && deploymentArtifacts.isEmpty()) {
            nodeTemplate.setDeploymentArtifacts(null);
        }
        final TPolicies policies = nodeTemplate.getPolicies();
        if ((policies != null) && policies.getPolicy().isEmpty()) {
            nodeTemplate.setPolicies(null);
        }
        final List<TArtifact> artifacts = nodeTemplate.getArtifacts();
        if ((artifacts != null) && artifacts.isEmpty()) {
            nodeTemplate.setArtifacts(null);
        }
        super.visit(nodeTemplate);
    }

    @Override
    public void visit(TRelationshipTemplate relationshipTemplate) {
        final TRelationshipTemplate.RelationshipConstraints relationshipConstraints = relationshipTemplate.getRelationshipConstraints();
        if ((relationshipConstraints != null) && relationshipConstraints.getRelationshipConstraint().isEmpty()) {
            relationshipTemplate.setRelationshipConstraints(null);
        }
        super.visit(relationshipTemplate);
    }

    /**
     * Removes the empty lists in the given topology template
     *
     * @param topologyTemplate the topology template to modify
     */
    public void removeEmptyLists(TTopologyTemplate topologyTemplate) {
        final TPolicies policies = topologyTemplate.getPolicies();
        if ((policies != null) && policies.getPolicy().isEmpty()) {
            topologyTemplate.setPolicies(null);
        }
        this.visit(topologyTemplate);
    }
}
