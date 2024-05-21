/********************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.yaml;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractResult;
import org.eclipse.winery.model.tosca.yaml.visitor.IVisitor;
import org.eclipse.winery.model.tosca.yaml.visitor.VisitorNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class YTTopologyTemplateDefinition implements VisitorNode {
    private String description;
    private Map<String, YTParameterDefinition> inputs;
    private Map<String, YTNodeTemplate> nodeTemplates;
    private Map<String, YTRelationshipTemplate> relationshipTemplates;
    private Map<String, YTGroupDefinition> groups;
    private Map<String, YTPolicyDefinition> policies;
    private Map<String, YTParameterDefinition> outputs;
    private YTSubstitutionMappings substitutionMappings;
    private Map<String, YTWorkflow> workflows;

    protected YTTopologyTemplateDefinition(Builder builder) {
        this.setDescription(builder.description);
        this.setInputs(builder.inputs);
        this.setNodeTemplates(builder.nodeTemplates);
        this.setRelationshipTemplates(builder.relationshipTemplates);
        this.setGroups(builder.groups);
        this.setPolicies(builder.policies);
        this.setOutputs(builder.outputs);
        this.setSubstitutionMappings(builder.substitutionMappings);
        this.setWorkflows(builder.workflows);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YTTopologyTemplateDefinition)) return false;
        YTTopologyTemplateDefinition that = (YTTopologyTemplateDefinition) o;
        return Objects.equals(getDescription(), that.getDescription()) &&
                Objects.equals(getInputs(), that.getInputs()) &&
                Objects.equals(getNodeTemplates(), that.getNodeTemplates()) &&
                Objects.equals(getRelationshipTemplates(), that.getRelationshipTemplates()) &&
                Objects.equals(getGroups(), that.getGroups()) &&
                Objects.equals(getPolicies(), that.getPolicies()) &&
                Objects.equals(getOutputs(), that.getOutputs()) &&
                Objects.equals(getSubstitutionMappings(), that.getSubstitutionMappings()) &&
                Objects.equals(getWorkflows(), that.getWorkflows());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getInputs(), getNodeTemplates(), getRelationshipTemplates(), getGroups(), getPolicies(), getOutputs(), getSubstitutionMappings(), getWorkflows());
    }

    @Override
    public String toString() {
        return "TTopologyTemplateDefinition{" +
                "description='" + getDescription() + '\'' +
                ", inputs=" + getInputs() +
                ", nodeTemplates=" + getNodeTemplates() +
                ", relationshipTemplates=" + getRelationshipTemplates() +
                ", groups=" + getGroups() +
                ", policies=" + getPolicies() +
                ", outputs=" + getOutputs() +
                ", substitutionMappings=" + getSubstitutionMappings() +
                ", workflows=" + getWorkflows() +
                '}';
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Nullable
    public Map<String, YTParameterDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, YTParameterDefinition> inputs) {
        this.inputs = inputs;
    }

    @NonNull
    public Map<String, YTNodeTemplate> getNodeTemplates() {
        if (this.nodeTemplates == null) {
            this.nodeTemplates = new LinkedHashMap<>();
        }

        return nodeTemplates;
    }

    public void setNodeTemplates(Map<String, YTNodeTemplate> nodeTemplates) {
        this.nodeTemplates = nodeTemplates;
    }

    @NonNull
    public Map<String, YTRelationshipTemplate> getRelationshipTemplates() {
        if (this.relationshipTemplates == null) {
            this.relationshipTemplates = new LinkedHashMap<>();
        }

        return relationshipTemplates;
    }

    public void setRelationshipTemplates(Map<String, YTRelationshipTemplate> relationshipTemplates) {
        this.relationshipTemplates = relationshipTemplates;
    }

    @NonNull
    public Map<String, YTGroupDefinition> getGroups() {
        if (this.groups == null) {
            this.groups = new LinkedHashMap<>();
        }

        return groups;
    }

    public void setGroups(Map<String, YTGroupDefinition> groups) {
        this.groups = groups;
    }

    @NonNull
    public Map<String, YTPolicyDefinition> getPolicies() {
        if (this.policies == null) {
            this.policies = new LinkedHashMap<>();
        }

        return policies;
    }

    public void setPolicies(Map<String, YTPolicyDefinition> policies) {
        this.policies = policies;
    }

    @Nullable
    public Map<String, YTParameterDefinition> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, YTParameterDefinition> outputs) {
        this.outputs = outputs;
    }

    @Nullable
    public YTSubstitutionMappings getSubstitutionMappings() {
        return substitutionMappings;
    }

    public void setSubstitutionMappings(YTSubstitutionMappings substitutionMappings) {
        this.substitutionMappings = substitutionMappings;
    }

    @Nullable
    public Map<String, YTWorkflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(Map<String, YTWorkflow> workflows) {
        this.workflows = workflows;
    }

    public <R extends AbstractResult<R>, P extends AbstractParameter<P>> R accept(IVisitor<R, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    public static class Builder {
        private String description;
        private Map<String, YTParameterDefinition> inputs;
        private Map<String, YTNodeTemplate> nodeTemplates;
        private Map<String, YTRelationshipTemplate> relationshipTemplates;
        private Map<String, YTGroupDefinition> groups;
        private Map<String, YTPolicyDefinition> policies;
        private Map<String, YTParameterDefinition> outputs;
        private YTSubstitutionMappings substitutionMappings;
        private Map<String, YTWorkflow> workflows;

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setInputs(Map<String, YTParameterDefinition> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder setNodeTemplates(Map<String, YTNodeTemplate> nodeTemplates) {
            this.nodeTemplates = nodeTemplates;
            return this;
        }

        public Builder setRelationshipTemplates(Map<String, YTRelationshipTemplate> relationshipTemplates) {
            this.relationshipTemplates = relationshipTemplates;
            return this;
        }

        public Builder setGroups(Map<String, YTGroupDefinition> groups) {
            this.groups = groups;
            return this;
        }

        public Builder setPolicies(Map<String, YTPolicyDefinition> policies) {
            this.policies = policies;
            return this;
        }

        public Builder setOutputs(Map<String, YTParameterDefinition> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Builder setSubstitutionMappings(YTSubstitutionMappings substitutionMappings) {
            this.substitutionMappings = substitutionMappings;
            return this;
        }

        public Builder setWorkflows(Map<String, YTWorkflow> workflows) {
            this.workflows = workflows;
            return this;
        }

        public Builder addInputs(Map<String, YTParameterDefinition> inputs) {
            if (inputs == null || inputs.isEmpty()) {
                return this;
            }

            if (this.inputs == null) {
                this.inputs = new LinkedHashMap<>(inputs);
            } else {
                this.inputs.putAll(inputs);
            }

            return this;
        }

        public Builder addInputs(String name, YTParameterDefinition input) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addInputs(Collections.singletonMap(name, input));
        }

        public Builder addNodeTemplates(Map<String, YTNodeTemplate> nodeTemplates) {
            if (nodeTemplates == null || nodeTemplates.isEmpty()) {
                return this;
            }

            if (this.nodeTemplates == null) {
                this.nodeTemplates = new LinkedHashMap<>(nodeTemplates);
            } else {
                this.nodeTemplates.putAll(nodeTemplates);
            }

            return this;
        }

        public Builder addNodeTemplates(String name, YTNodeTemplate nodeTemplate) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addNodeTemplates(Collections.singletonMap(name, nodeTemplate));
        }

        public Builder addRelationshipTemplates(Map<String, YTRelationshipTemplate> relationshipTemplates) {
            if (relationshipTemplates == null || relationshipTemplates.isEmpty()) {
                return this;
            }

            if (this.relationshipTemplates == null) {
                this.relationshipTemplates = new LinkedHashMap<>(relationshipTemplates);
            } else {
                this.relationshipTemplates.putAll(relationshipTemplates);
            }

            return this;
        }

        public Builder addRelationshipTemplates(String name, YTRelationshipTemplate relationshipTemplate) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addRelationshipTemplates(Collections.singletonMap(name, relationshipTemplate));
        }

        public Builder addGroups(Map<String, YTGroupDefinition> groups) {
            if (groups == null || groups.isEmpty()) {
                return this;
            }

            if (this.groups == null) {
                this.groups = new LinkedHashMap<>(groups);
            } else {
                this.groups.putAll(groups);
            }

            return this;
        }

        public Builder addGroups(String name, YTGroupDefinition group) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addGroups(Collections.singletonMap(name, group));
        }

        public Builder addPolicies(Map<String, YTPolicyDefinition> policies) {
            if (policies == null || policies.isEmpty()) {
                return this;
            }

            if (this.policies == null) {
                this.policies = new LinkedHashMap<>(policies);
            } else {
                this.policies.putAll(policies);
            }

            return this;
        }

        public Builder addPolicies(String name, YTPolicyDefinition policy) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addPolicies(Collections.singletonMap(name, policy));
        }

        public Builder addOutputs(Map<String, YTParameterDefinition> outputs) {
            if (outputs == null || outputs.isEmpty()) {
                return this;
            }

            if (this.outputs == null) {
                this.outputs = new LinkedHashMap<>(outputs);
            } else {
                this.outputs.putAll(outputs);
            }

            return this;
        }

        public Builder addOutputs(String name, YTParameterDefinition output) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addOutputs(Collections.singletonMap(name, output));
        }

        public YTTopologyTemplateDefinition build() {
            return new YTTopologyTemplateDefinition(this);
        }
    }
}
