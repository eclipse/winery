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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.model.tosca.visitor.Visitor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tNodeTemplate", propOrder = {
    "requirements",
    "capabilities",
    "policies",
    "deploymentArtifacts",
    "artifacts"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
    defaultImpl = TNodeTemplate.class,
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "fakeJacksonType")
public class TNodeTemplate extends RelationshipSourceOrTarget implements HasPolicies {

    @XmlElementWrapper(name = "Requirements")
    @XmlElement(name = "Requirement")
    protected List<TRequirement> requirements;
    
    @XmlElementWrapper(name = "Capabilities")
    @XmlElement(name = "Capability")
    protected List<TCapability> capabilities;
    
    @XmlElement(name = "Policies")
    protected TPolicies policies;
    
    @XmlElementWrapper(name = "DeploymentArtifacts")
    @XmlElement(name = "DeploymentArtifact", required = true)
    protected List<TDeploymentArtifact> deploymentArtifacts;
    
    @XmlAttribute(name = "name")
    protected String name;
    
    @XmlAttribute(name = "minInstances")
    protected Integer minInstances;
    
    @XmlAttribute(name = "maxInstances")
    protected String maxInstances;
    
    // this element is added to support YAML mode
    @XmlElement(name = "Artifacts")
    protected List<TArtifact> artifacts;

    @Deprecated // used for XML deserialization of API request content
    public TNodeTemplate() {
        super();
    }

    public TNodeTemplate(String id) {
        super(id);
    }

    public TNodeTemplate(Builder builder) {
        super(builder);
        this.requirements = builder.requirements;
        this.capabilities = builder.capabilities;
        this.policies = builder.policies;
        this.deploymentArtifacts = builder.deploymentArtifacts;
        this.name = builder.name;
        this.minInstances = builder.minInstances;
        this.maxInstances = builder.maxInstances;
        this.artifacts = builder.artifacts;

        if (Objects.nonNull(builder.x) && Objects.nonNull(builder.y)) {
            this.setX(builder.x);
            this.setY(builder.y);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TNodeTemplate)) return false;
        if (!super.equals(o)) return false;
        TNodeTemplate that = (TNodeTemplate) o;
        return Objects.equals(requirements, that.requirements) &&
            Objects.equals(capabilities, that.capabilities) &&
            Objects.equals(policies, that.policies) &&
            Objects.equals(deploymentArtifacts, that.deploymentArtifacts) &&
            Objects.equals(name, that.name) &&
            Objects.equals(minInstances, that.minInstances) &&
            Objects.equals(maxInstances, that.maxInstances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), requirements, capabilities, policies, deploymentArtifacts, name, minInstances, maxInstances);
    }

    @Override
    @NonNull
    public String getFakeJacksonType() {
        return "nodetemplate";
    }

    public List<TRequirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<TRequirement> value) {
        this.requirements = value;
    }

    public List<TCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<TCapability> value) {
        this.capabilities = value;
    }

    public @Nullable TPolicies getPolicies() {
        return policies;
    }

    public void setPolicies(@Nullable TPolicies value) {
        this.policies = value;
    }

    @Nullable
    public List<TDeploymentArtifact> getDeploymentArtifacts() {
        return deploymentArtifacts;
    }

    public void setDeploymentArtifacts(List<TDeploymentArtifact> value) {
        this.deploymentArtifacts = value;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String value) {
        this.name = value;
    }

    public int getMinInstances() {
        if (minInstances == null) {
            return 1;
        } else {
            return minInstances;
        }
    }

    public void setMinInstances(Integer value) {
        this.minInstances = value;
    }

    @NonNull
    public String getMaxInstances() {
        if (maxInstances == null) {
            return "1";
        } else {
            return maxInstances;
        }
    }

    public void setMaxInstances(String value) {
        this.maxInstances = value;
    }

    /**
     * In the JSON, also output this direct child of the node template object. Therefore, no JsonIgnore annotation.
     */
    @XmlTransient
    @Nullable
    public String getX() {
        Map<QName, String> otherNodeTemplateAttributes = this.getOtherAttributes();
        return otherNodeTemplateAttributes.get(new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "x"));
    }

    /**
     * Sets the top coordinate of a {@link TNodeTemplate}. When receiving the JSON, this method ensures that (i) the "y"
     * property can be handled and (ii) the Y coordinate is written correctly in the extension namespace.
     *
     * @param x the value of the x-coordinate to be set
     */
    public void setX(@NonNull String x) {
        Objects.requireNonNull(x);
        Map<QName, String> otherNodeTemplateAttributes = this.getOtherAttributes();
        otherNodeTemplateAttributes.put(new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "x"), x);
    }

    /**
     * In the JSON, also output this direct child of the node template object. Therefore, no JsonIgnore annotation.
     */
    @XmlTransient
    @Nullable
    public String getY() {
        Map<QName, String> otherNodeTemplateAttributes = this.getOtherAttributes();
        return otherNodeTemplateAttributes.get(new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "y"));
    }

    /**
     * Sets the top coordinate of a {@link TNodeTemplate}. When receiving the JSON, this method ensures that (i) the "y"
     * property can be handled and (ii) the Y coordinate is written correctly in the extension namespace.
     *
     * @param y the value of the coordinate to be set
     */
    public void setY(@NonNull String y) {
        Map<QName, String> otherNodeTemplateAttributes = this.getOtherAttributes();
        otherNodeTemplateAttributes.put(new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "y"), y);
    }

    public void accept(@NonNull Visitor visitor) {
        visitor.visit(this);
    }

    public List<TArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<TArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    public static class Builder extends RelationshipSourceOrTarget.Builder<Builder> {
        
        private List<TRequirement> requirements;
        private List<TCapability> capabilities;
        private TPolicies policies;
        private List<TDeploymentArtifact> deploymentArtifacts;
        private String name;
        private Integer minInstances;
        private String maxInstances;
        private String x;
        private String y;
        private List<TArtifact> artifacts;

        public Builder(String id, QName type) {
            super(id, type);
        }

        public Builder(TEntityTemplate entityTemplate) {
            super(entityTemplate);
        }

        public Builder setRequirements(List<TRequirement> requirements) {
            this.requirements = requirements;
            return this;
        }

        public Builder setCapabilities(List<TCapability> capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder setPolicies(TPolicies policies) {
            this.policies = policies;
            return this;
        }

        public Builder addDeploymentArtifacts(List<TDeploymentArtifact> deploymentArtifacts) {
            if (deploymentArtifacts == null || deploymentArtifacts.isEmpty()) {
                return this;
            }

            if (this.deploymentArtifacts == null) {
                this.deploymentArtifacts = deploymentArtifacts;
            } else {
                this.deploymentArtifacts.addAll(deploymentArtifacts);
            }
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setMinInstances(Integer minInstances) {
            this.minInstances = minInstances;
            return this;
        }

        public Builder setMaxInstances(String maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        public Builder setX(String x) {
            this.x = x;
            return this;
        }

        public Builder setY(String y) {
            this.y = y;
            return this;
        }

        public Builder addRequirements(List<TRequirement> requirements) {
            if (requirements == null || requirements.isEmpty()) {
                return this;
            }

            if (this.requirements == null) {
                this.requirements = requirements;
            } else {
                this.requirements.addAll(requirements);
            }
            return this;
        }

        public void addRequirement(TRequirement requirements) {
            if (requirements == null) {
                return;
            }

            List<TRequirement> tmp = new ArrayList<>();
            tmp.add(requirements);
            addRequirements(tmp);
        }

        public Builder addCapabilities(List<TCapability> capabilities) {
            if (capabilities == null || capabilities.isEmpty()) {
                return this;
            }

            if (this.capabilities == null) {
                this.capabilities = capabilities;
            } else {
                this.capabilities.addAll(capabilities);
            }
            return this;
        }

        public void addCapability(TCapability capabilities) {
            if (capabilities == null) {
                return;
            }

            ArrayList<TCapability> tmp = new ArrayList<>();
            tmp.add(capabilities);
            addCapabilities(tmp);
        }

        public Builder addPolicies(TPolicies policies) {
            if (policies == null) {
                return this;
            }

            if (this.policies == null) {
                this.policies = policies;
            } else {
                this.policies.getPolicy().addAll(policies.getPolicy());
            }
            return this;
        }

        public Builder addPolicies(List<TPolicy> policies) {
            if (policies == null) {
                return this;
            }

            TPolicies tmp = new TPolicies();
            tmp.getPolicy().addAll(policies);
            return addPolicies(tmp);
        }

        public Builder addPolicies(TPolicy policies) {
            if (policies == null) {
                return this;
            }

            TPolicies tmp = new TPolicies();
            tmp.getPolicy().add(policies);
            return addPolicies(tmp);
        }
        
        public Builder setArtifacts(List<TArtifact> artifacts) {
            this.artifacts = artifacts;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        public TNodeTemplate build() {
            return new TNodeTemplate(this);
        }

        public Builder addDeploymentArtifact(TDeploymentArtifact deploymentArtifact) {
            if (deploymentArtifact == null) {
                return this;
            }

            List<TDeploymentArtifact> tmp = new ArrayList<>();
            tmp.add(deploymentArtifact);
            return addDeploymentArtifacts(tmp);
        }
    }
}
