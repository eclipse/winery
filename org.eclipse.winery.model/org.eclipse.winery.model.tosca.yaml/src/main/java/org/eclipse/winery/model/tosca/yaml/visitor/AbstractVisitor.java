/********************************************************************************
 * Copyright (c) 2017-2021 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.yaml.visitor;

import org.eclipse.jdt.annotation.NonNull;

import org.eclipse.winery.model.tosca.yaml.YTArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.YTArtifactType;
import org.eclipse.winery.model.tosca.yaml.YTAttributeAssignment;
import org.eclipse.winery.model.tosca.yaml.YTAttributeDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCallOperationActivityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityAssignment;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityType;
import org.eclipse.winery.model.tosca.yaml.YTConstraintClause;
import org.eclipse.winery.model.tosca.yaml.YTDataType;
import org.eclipse.winery.model.tosca.yaml.YTEntityType;
import org.eclipse.winery.model.tosca.yaml.YTEventFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTGroupDefinition;
import org.eclipse.winery.model.tosca.yaml.YTGroupType;
import org.eclipse.winery.model.tosca.yaml.YTImplementation;
import org.eclipse.winery.model.tosca.yaml.YTImportDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceAssignment;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceType;
import org.eclipse.winery.model.tosca.yaml.YTNodeFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTNodeTemplate;
import org.eclipse.winery.model.tosca.yaml.YTNodeType;
import org.eclipse.winery.model.tosca.yaml.YTOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.YTParameterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyType;
import org.eclipse.winery.model.tosca.yaml.YTPropertyAssignment;
import org.eclipse.winery.model.tosca.yaml.YTPropertyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPropertyFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipAssignment;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipTemplate;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipType;
import org.eclipse.winery.model.tosca.yaml.YTRepositoryDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.YTRequirementDefinition;
import org.eclipse.winery.model.tosca.yaml.YTSchemaDefinition;
import org.eclipse.winery.model.tosca.yaml.YTServiceTemplate;
import org.eclipse.winery.model.tosca.yaml.YTSubstitutionMappings;
import org.eclipse.winery.model.tosca.yaml.YTTopologyTemplateDefinition;
import org.eclipse.winery.model.tosca.yaml.YTTriggerDefinition;
import org.eclipse.winery.model.tosca.yaml.YTVersion;
import org.eclipse.winery.model.tosca.yaml.YTWorkflow;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractVisitor<R extends AbstractResult<R>, P extends AbstractParameter<P>> implements IVisitor<R, P> {
    @Override
    public R visit(YTArtifactDefinition node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTArtifactType node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTAttributeAssignment node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTAttributeDefinition node, P parameter) {
        return visitElement(node.getEntrySchema(), parameter, "entry_schema");
    }

    @Override
    public R visit(YTCallOperationActivityDefinition node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTTriggerDefinition node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTEventFilterDefinition node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTCapabilityAssignment node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getProperties(), parameter, "properties"),
                visitElement(node.getAttributes(), parameter, "attributes")
        ));
    }

    @Override
    public R visit(YTCapabilityDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getProperties(), parameter, "properties"),
                visitElement(node.getAttributes(), parameter, "attributes")
        ));
    }

    @Override
    public R visit(YTCapabilityType node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTConstraintClause node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTDataType node, P parameter) {
        return visitElement(node.getConstraints(), parameter, "constraints");
    }

    @Override
    public R visit(YTEntityType node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getVersion(), parameter, "version"),
                visitElement(node.getProperties(), parameter, "properties"),
                visitElement(node.getAttributes(), parameter, "attributes"),
                visitElement(node.getMetadata(), parameter, "metadata")
        ));
    }

    @Override
    public R visit(YTSchemaDefinition node, P parameter) {
        return visitElement(node.getConstraints(), parameter, "constraints");
    }

    @Override
    public R visit(YTGroupDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getMetadata(), parameter, "metadata"),
                visitElement(node.getProperties(), parameter, "properties")
        ));
    }

    @Override
    public R visit(YTGroupType node, P parameter) {
        return reduce(Stream.of(
                // TODO may be removed
        ));
    }

    @Override
    public R visit(YTImplementation node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTImportDefinition node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTInterfaceAssignment node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTInterfaceDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getInputs(), parameter, "inputs"),
                visitElement(node.getOperations(), parameter, "operations")
        ));
    }

    @Override
    public R visit(YTInterfaceType node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getInputs(), parameter, "inputs"),
                visitElement(node.getOperations(), parameter, "operations")
        ));
    }

    @Override
    public R visit(YTNodeFilterDefinition node, P parameter) {
        return visitMapElement(node.getProperties(), parameter, "properties");
    }

    @Override
    public R visit(YTNodeTemplate node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getMetadata(), parameter, "metadata"),
                visitElement(node.getProperties(), parameter, "properties"),
                visitElement(node.getAttributes(), parameter, "attributes"),
                visitMapElement(node.getRequirements(), parameter, "requirements"),
                visitElement(node.getCapabilities(), parameter, "capabilities"),
                visitElement(node.getArtifacts(), parameter, "artifacts"),
                visitElement(node.getInterfaces(), parameter, "interfaces"),
                visitElement(node.getNodeFilter(), parameter, "node_filter")
        ));
    }

    @Override
    public R visit(YTNodeType node, P parameter) {
        return reduce(Stream.of(
                visitMapElement(node.getRequirements(), parameter, "requirements"),
                visitElement(node.getInterfaces(), parameter, "interfaces"),
                visitElement(node.getArtifacts(), parameter, "artifacts")
        ));
    }

    @Override
    public R visit(YTOperationDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getInputs(), parameter, "inputs"),
                visitElement(node.getImplementation(), parameter, "implementation")
        ));
    }

    @Override
    public R visit(YTWorkflow node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getInputs(), parameter, "inputs"),
                visitElement(node.getImplementation(), parameter, "implementation")
        ));
    }

    @Override
    public R visit(YTParameterDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getConstraints(), parameter, "constraints"),
                visitElement(node.getEntrySchema(), parameter, "entry_schema")
        ));
    }

    @Override
    public R visit(YTPolicyDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getMetadata(), parameter, "metadata"),
                visitElement(node.getProperties(), parameter, "properties")
        ));
    }

    @Override
    public R visit(YTPolicyType node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTPropertyAssignment node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTPropertyDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getConstraints(), parameter, "constraints"),
                visitElement(node.getEntrySchema(), parameter, "entry_schema")
        ));
    }

    @Override
    public R visit(YTPropertyFilterDefinition node, P parameter) {
        return visitElement(node.getConstraints(), parameter, "constraints");
    }

    @Override
    public R visit(YTRelationshipAssignment node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getProperties(), parameter, "properties"),
                visitElement(node.getInterfaces(), parameter, "interfaces")
        ));
    }

    @Override
    public R visit(YTRelationshipDefinition node, P parameter) {
        return visitElement(node.getInterfaces(), parameter, "interfaces");
    }

    @Override
    public R visit(YTRelationshipTemplate node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getMetadata(), parameter, "metadata"),
                visitElement(node.getProperties(), parameter, "properties"),
                visitElement(node.getAttributes(), parameter, "attributes"),
                visitElement(node.getInterfaces(), parameter, "interfaces")
        ));
    }

    @Override
    public R visit(YTRelationshipType node, P parameter) {
        return visitElement(node.getInterfaces(), parameter, "interfaces");
    }

    @Override
    public R visit(YTRepositoryDefinition node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTRequirementAssignment node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getRelationship(), parameter, "relationship"),
                visitElement(node.getNodeFilter(), parameter, "node_filter")
        ));
    }

    @Override
    public R visit(YTRequirementDefinition node, P parameter) {
        return visitElement(node.getRelationship(), parameter, "constraints");
    }

    @Override
    public R visit(YTServiceTemplate node, P parameter) {
        return reduce(Stream.of(
                node.getMetadata().accept(this, parameter.copy().addContext("metadata")),
                visitElement(node.getRepositories(), parameter, "repositories"),
                visitMapElement(node.getImports(), parameter, "imports"),
                visitElement(node.getArtifactTypes(), parameter, "artifact_types"),
                visitElement(node.getDataTypes(), parameter, "data_types"),
                visitElement(node.getCapabilityTypes(), parameter, "capability_types"),
                visitElement(node.getInterfaceTypes(), parameter, "interface_types"),
                visitElement(node.getRelationshipTypes(), parameter, "relationship_types"),
                visitElement(node.getNodeTypes(), parameter, "node_types"),
                visitElement(node.getGroupTypes(), parameter, "group_types"),
                visitElement(node.getPolicyTypes(), parameter, "policy_types"),
                visitElement(node.getTopologyTemplate(), parameter, "topology_template")
        ));
    }

    @Override
    public R visit(YTSubstitutionMappings node, P parameter) {
        return null;
    }

    @Override
    public R visit(YTTopologyTemplateDefinition node, P parameter) {
        return reduce(Stream.of(
                visitElement(node.getInputs(), parameter, "inputs"),
                visitElement(node.getNodeTemplates(), parameter, "node_templates"),
                visitElement(node.getRelationshipTemplates(), parameter, "relationship_templates"),
                visitElement(node.getGroups(), parameter, "groups"),
                visitElement(node.getPolicies(), parameter, "policies"),
                visitElement(node.getOutputs(), parameter, "outputs"),
                visitElement(node.getSubstitutionMappings(), parameter, "substitution_mappings")
        ));
    }

    @Override
    public R visit(YTVersion node, P parameter) {
        return null;
    }

    @Override
    public R visit(Metadata node, P parameter) {
        return null;
    }

    private R visitMapElement(@NonNull List<? extends Map<String, ? extends VisitorNode>> list, P parameter, String name) {
        return list.stream()
                .filter(Objects::nonNull)
                .flatMap(map -> map.entrySet().stream())
                .filter(this::nonNull)
                .map(entry -> entry.getValue().accept(this, parameter.copy().addContext(name, entry.getKey())))
                .filter(Objects::nonNull)
                .reduce(this::addR).orElse(null);
    }

    private R visitElement(Metadata node, P parameter, String name) {
        return Optional.ofNullable(node)
                .map(entry -> entry.accept(this, parameter.copy().addContext(name)))
                .orElse(null);
    }

    private R visitElement(VisitorNode node, P parameter, String name) {
        return Optional.ofNullable(node)
                .map(entry -> entry.accept(this, parameter.copy().addContext(name)))
                .orElse(null);
    }

    private R visitElement(@NonNull List<? extends VisitorNode> list, P parameter, String name) {
        return list.stream()
                .filter(Objects::nonNull)
                .map(entry -> entry.accept(this, parameter.copy().addContext(name)))
                .filter(Objects::nonNull)
                .reduce(this::addR)
                .orElse(null);
    }

    private R visitElement(@NonNull Map<String, ? extends VisitorNode> map, P parameter, String name) {
        return map.entrySet().stream()
                .filter(this::nonNull)
                .map(entry -> entry.getValue().accept(this, parameter.copy().addContext(name, entry.getKey())))
                .filter(Objects::nonNull)
                .reduce(this::addR).orElse(null);
    }

    private <K> Boolean nonNull(Map.Entry<String, K> entry) {
        return Objects.nonNull(entry) && Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue());
    }

    private R reduce(Stream<R> stream) {
        return stream
                .filter(Objects::nonNull)
                .reduce(this::addR)
                .orElse(null);
    }

    private R addR(R r1, R r2) {
        if (Objects.isNull(r1)) {
            return r2;
        }
        return r1.add(r2);
    }
}
