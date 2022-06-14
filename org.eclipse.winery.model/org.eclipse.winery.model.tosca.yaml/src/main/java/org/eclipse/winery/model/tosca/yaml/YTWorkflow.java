/*******************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

public class YTWorkflow implements VisitorNode {
    private String name;
    private String description;
    private Map<String, YTParameterDefinition> inputs;
    private Map<String, YTParameterDefinition> outputs;
    private YTImplementation implementation;

    private YTWorkflow(Builder builder) {
        this.setName(builder.name);
        this.setDescription(builder.description);
        this.setInputs(builder.inputs);
        this.setOutputs(builder.outputs);
        this.setImplementation(builder.implementation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YTWorkflow)) return false;
        YTWorkflow that = (YTWorkflow) o;
        return Objects.equals(getName(), that.getName()) &&
            Objects.equals(getDescription(), that.getDescription()) &&
            Objects.equals(getInputs(), that.getInputs()) &&
            Objects.equals(getOutputs(), that.getOutputs()) &&
            Objects.equals(getImplementation(), that.getImplementation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDescription(), getInputs(), getOutputs(), getImplementation());
    }

    @Override
    public String toString() {
        return "YTWorkflow{" +
            "name='" + getName() + '\'' +
            ", description='" + getDescription() + '\'' +
            ", inputs=" + getInputs() +
            ", outputs=" + getOutputs() +
            ", implementation=" + getImplementation() +
            '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NonNull
    public Map<String, YTParameterDefinition> getInputs() {
        if (this.inputs == null) {
            this.inputs = new LinkedHashMap<>();
        }

        return inputs;
    }

    public void setInputs(Map<String, YTParameterDefinition> inputs) {
        this.inputs = inputs;
    }

    @NonNull
    public Map<String, YTParameterDefinition> getOutputs() {
        if (this.outputs == null) {
            this.outputs = new LinkedHashMap<>();
        }
        return outputs;
    }

    public void setOutputs(Map<String, YTParameterDefinition> outputs) {
        this.outputs = outputs;
    }

    @Nullable
    public YTImplementation getImplementation() {
        return implementation;
    }

    public void setImplementation(YTImplementation implementation) {
        this.implementation = implementation;
    }
    
    public <R extends AbstractResult<R>, P extends AbstractParameter<P>> R accept(IVisitor<R, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    public static class Builder {

        private String name;
        private String description;
        private Map<String, YTParameterDefinition> inputs;
        private Map<String, YTParameterDefinition> outputs;
        private YTImplementation implementation;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setInputs(Map<String, YTParameterDefinition> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder setOutputs(Map<String, YTParameterDefinition> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Builder setImplementation(YTImplementation implementation) {
            this.implementation = implementation;
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

        public YTWorkflow build() {
            return new YTWorkflow(this);
        }
    }
}
