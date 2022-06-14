/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.tosca;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import org.eclipse.winery.model.tosca.extensions.OTParticipant;
import org.eclipse.winery.model.tosca.extensions.kvproperties.ParameterDefinition;
import org.eclipse.winery.model.tosca.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tTopologyTemplate", propOrder = {
        "nodeTemplateOrRelationshipTemplate",
        "groups",
        "policies",
        "inputs",
        "outputs",
        "participants",
        "workflows"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TTopologyTemplate extends TExtensibleElements {
    @XmlElements( {
            @XmlElement(name = "RelationshipTemplate", type = TRelationshipTemplate.class),
            @XmlElement(name = "NodeTemplate", type = TNodeTemplate.class)
    })
    protected List<TEntityTemplate> nodeTemplateOrRelationshipTemplate;

    // added to support conversion from/to YAML policies
    @JsonProperty("groups")
    protected List<TGroupDefinition> groups;
    @JsonProperty("policies")
    protected List<TPolicy> policies;
    @JsonProperty("inputs")
    protected List<ParameterDefinition> inputs;
    @JsonProperty("outputs")
    protected List<ParameterDefinition> outputs;
    // added to support conversion from/to YAML workflows
    @JsonProperty("workflows")
    protected List<TWorkflow> workflows;

    @XmlElementWrapper(name = "Participants")
    @XmlElement(name = "Participant")
    protected List<OTParticipant> participants;

    @Deprecated // used for XML deserialization of API request content
    public TTopologyTemplate() {
    }

    public TTopologyTemplate(Builder builder) {
        super(builder);
        this.nodeTemplateOrRelationshipTemplate = builder.getNodeTemplateOrRelationshipTemplate();
        this.policies = builder.policies;
        this.inputs = builder.inputs;
        this.outputs = builder.outputs;
        this.groups = builder.groups;
        this.participants = builder.participants;
        this.workflows = builder.workflows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TTopologyTemplate)) return false;
        if (!super.equals(o)) return false;
        TTopologyTemplate that = (TTopologyTemplate) o;
        return Objects.equals(nodeTemplateOrRelationshipTemplate, that.nodeTemplateOrRelationshipTemplate) &&
                Objects.equals(policies, that.policies) &&
                Objects.equals(inputs, that.inputs) &&
                Objects.equals(outputs, that.outputs) &&
                Objects.equals(workflows, that.workflows) &&
                Objects.equals(participants, that.participants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeTemplateOrRelationshipTemplate, policies, inputs, outputs, workflows, participants);
    }

    @JsonIgnore
    @NonNull
    public List<TEntityTemplate> getNodeTemplateOrRelationshipTemplate() {
        if (nodeTemplateOrRelationshipTemplate == null) {
            nodeTemplateOrRelationshipTemplate = new ArrayList<>();
        }
        return this.nodeTemplateOrRelationshipTemplate;
    }

    @JsonIgnore
    @Nullable
    public TEntityTemplate getNodeTemplateOrRelationshipTemplate(String id) {
        Objects.requireNonNull(id);
        return this.getNodeTemplateOrRelationshipTemplate().stream()
                .filter(x -> id.equals(x.getId()))
                .findAny()
                .orElse(null);
    }

    /**
     * @return all nodes templates of the topologyTemplate
     */
    @NonNull
    public List<TNodeTemplate> getNodeTemplates() {
        return this.getNodeTemplateOrRelationshipTemplate()
                .stream()
                .filter(x -> x instanceof TNodeTemplate)
                .map(TNodeTemplate.class::cast)
                .collect(Collectors.toList());
    }

    public void setNodeTemplates(List<TNodeTemplate> nodeTemplates) {
        this.nodeTemplateOrRelationshipTemplate = Stream.concat(
                        nodeTemplates.stream().map(TEntityTemplate.class::cast),
                        this.getRelationshipTemplates().stream().map(TEntityTemplate.class::cast))
                .collect(Collectors.toList());
    }

    /**
     * @return node template having the given id. null if not found
     */
    @Nullable
    public TNodeTemplate getNodeTemplate(String id) {
        Objects.requireNonNull(id);
        return this.getNodeTemplates().stream()
                .filter(x -> id.equals(x.getId()))
                .findAny()
                .orElse(null);
    }

    /**
     * @return all relationship templates of the topologyTemplate
     */
    @NonNull
    public List<TRelationshipTemplate> getRelationshipTemplates() {
        return this.getNodeTemplateOrRelationshipTemplate()
                .stream()
                .filter(x -> x instanceof TRelationshipTemplate)
                .map(TRelationshipTemplate.class::cast)
                .collect(Collectors.toList());
    }

    public void setRelationshipTemplates(List<TRelationshipTemplate> relationshipTemplates) {
        this.nodeTemplateOrRelationshipTemplate = Stream.concat(
                        this.getNodeTemplates().stream().map(TEntityTemplate.class::cast),
                        relationshipTemplates.stream().map(TEntityTemplate.class::cast))
                .collect(Collectors.toList());
    }

    /**
     * @return relationship template having the given id. null if not found
     */
    @Nullable
    public TRelationshipTemplate getRelationshipTemplate(String id) {
        Objects.requireNonNull(id);
        return this.getRelationshipTemplates().stream()
                .filter(x -> id.equals(x.getId()))
                .findAny()
                .orElse(null);
    }

    public void addNodeTemplate(TNodeTemplate nt) {
        this.getNodeTemplateOrRelationshipTemplate().add(nt);
    }

    public void addRelationshipTemplate(TRelationshipTemplate rt) {
        this.getNodeTemplateOrRelationshipTemplate().add(rt);
    }

    @Nullable
    public List<TPolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<TPolicy> policies) {
        this.policies = policies;
    }

    @Nullable
    public List<ParameterDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(List<ParameterDefinition> inputs) {
        this.inputs = inputs;
    }

    @Nullable
    public List<ParameterDefinition> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ParameterDefinition> outputs) {
        this.outputs = outputs;
    }

    @NonNull
    public List<TWorkflow> getWorkflows() {
        if (this.workflows == null) {
            this.workflows = new ArrayList<>();
        }

        return workflows;
    }

    public void setWorkflows(List<TWorkflow> workflows) {
        this.workflows = workflows;
    }

    @Nullable
    public List<TGroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(List<TGroupDefinition> groups) {
        this.groups = groups;
    }

    public void addGroup(TGroupDefinition group) {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        groups.add(group);
    }

    @Nullable
    public List<OTParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(@Nullable List<OTParticipant> participants) {
        this.participants = participants;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public static class Builder extends TExtensibleElements.Builder<Builder> {
        private List<TNodeTemplate> nodeTemplates;
        private List<TRelationshipTemplate> relationshipTemplates;
        private List<TPolicy> policies;
        private List<ParameterDefinition> inputs;
        private List<ParameterDefinition> outputs;
        private List<TWorkflow> workflows;
        private List<TGroupDefinition> groups;
        private List<OTParticipant> participants;

        public Builder() {
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder setNodeTemplates(List<TNodeTemplate> nodeTemplates) {
            this.nodeTemplates = nodeTemplates;
            return this;
        }

        public Builder setRelationshipTemplates(List<TRelationshipTemplate> relationshipTemplates) {
            this.relationshipTemplates = relationshipTemplates;
            return this;
        }

        public Builder addNodeTemplates(List<TNodeTemplate> nodeTemplates) {
            if (nodeTemplates == null || nodeTemplates.isEmpty()) {
                return this;
            }

            if (this.nodeTemplates == null) {
                this.nodeTemplates = nodeTemplates;
            } else {
                this.nodeTemplates.addAll(nodeTemplates);
            }
            return this;
        }

        public Builder addNodeTemplate(TNodeTemplate nodeTemplate) {
            if (nodeTemplate == null) {
                return this;
            }

            List<TNodeTemplate> tmp = new ArrayList<>();
            tmp.add(nodeTemplate);
            return addNodeTemplates(tmp);
        }

        public Builder addRelationshipTemplates(List<TRelationshipTemplate> relationshipTemplates) {
            if (relationshipTemplates == null || relationshipTemplates.isEmpty()) {
                return this;
            }

            if (this.relationshipTemplates == null) {
                this.relationshipTemplates = relationshipTemplates;
            } else {
                this.relationshipTemplates.addAll(relationshipTemplates);
            }
            return this;
        }

        public Builder addRelationshipTemplate(TRelationshipTemplate relationshipTemplates) {
            if (relationshipTemplates == null) {
                return this;
            }

            List<TRelationshipTemplate> tmp = new ArrayList<>();
            tmp.add(relationshipTemplates);
            return addRelationshipTemplates(tmp);
        }

        public Builder setPolicies(List<TPolicy> policies) {
            this.policies = policies;
            return this;
        }

        public Builder setInputs(List<ParameterDefinition> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder setOutputs(List<ParameterDefinition> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Builder setWorkflows(List<TWorkflow> workflows) {
            this.workflows = workflows;
            return this;
        }

        public Builder addWorkflows(List<TWorkflow> workflows) {
            if (workflows == null || workflows.isEmpty()) {
                return this;
            }

            if (this.workflows == null) {
                this.workflows = new ArrayList<>(workflows);
            } else {
                this.workflows.addAll(workflows);
            }

            return this;
        }

        public Builder setGroups(List<TGroupDefinition> groups) {
            this.groups = groups;
            return this;
        }

        public Builder setParticipants(List<OTParticipant> participants) {
            this.participants = participants;
            return this;
        }

        public List<TEntityTemplate> getNodeTemplateOrRelationshipTemplate() {
            List<TEntityTemplate> tmp = new ArrayList<>();
            Optional.ofNullable(nodeTemplates).ifPresent(tmp::addAll);
            Optional.ofNullable(relationshipTemplates).ifPresent(tmp::addAll);
            return tmp;
        }

        public TTopologyTemplate build() {
            return new TTopologyTemplate(this);
        }
    }
}
