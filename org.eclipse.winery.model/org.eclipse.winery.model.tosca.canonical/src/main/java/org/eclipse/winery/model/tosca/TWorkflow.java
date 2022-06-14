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

package org.eclipse.winery.model.tosca;

import org.eclipse.winery.model.tosca.extensions.kvproperties.ParameterDefinition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Workflow", propOrder = {
    "name",
    "description",
    "inputs",
    "outputs",
    "implementation",
})
public class TWorkflow implements HasName, Serializable {

    private String name;
    private String description;
    private List<ParameterDefinition> inputs;
    private List<ParameterDefinition> outputs;
    private TImplementation implementation;

    @Deprecated // used for XML deserialization of API request content
    public TWorkflow() {
    }

    private TWorkflow(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.inputs = builder.inputs;
        this.outputs = builder.outputs;
        this.implementation = builder.implementation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ParameterDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(List<ParameterDefinition> inputs) {
        this.inputs = inputs;
    }

    public List<ParameterDefinition> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ParameterDefinition> outputs) {
        this.outputs = outputs;
    }

    public TImplementation getImplementation() {
        return implementation;
    }

    public void setImplementation(TImplementation implementation) {
        this.implementation = implementation;
    }

    public static class Builder {
        private final String name;
        private String description;
        private List<ParameterDefinition> inputs;
        private List<ParameterDefinition> outputs;
        private TImplementation implementation;

        public Builder(String name) {
            this.name = name;
        }

        public Builder setDescription(String description) {
            this.description = description;
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

        public Builder setImplementation(TImplementation implementation) {
            this.implementation = implementation;
            return this;
        }

        public TWorkflow build() {
            return new TWorkflow(this);
        }
    }
}
