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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.yaml.support.Annotations;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractResult;
import org.eclipse.winery.model.tosca.yaml.visitor.IVisitor;
import org.eclipse.winery.model.tosca.yaml.visitor.VisitorNode;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class YTRequirementDefinition implements VisitorNode {
    private QName capability;
    private QName node;
    private YTRelationshipDefinition relationship;
    private List<String> occurrences;

    @Annotations.StandardExtension
    private String description;

    protected YTRequirementDefinition(Builder builder) {
        this.setCapability(builder.capability);
        this.setOccurrences(builder.occurrences);
        this.setNode(builder.node);
        this.setRelationship(builder.relationship);
        this.setDescription(builder.description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YTRequirementDefinition)) return false;
        YTRequirementDefinition that = (YTRequirementDefinition) o;
        return Objects.equals(getCapability(), that) &&
            Objects.equals(getNode(), that.getNode()) &&
            Objects.equals(getRelationship(), that.getRelationship()) &&
            Objects.equals(getOccurrences(), that.getOccurrences()) &&
            Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCapability(), getNode(), getRelationship(), getOccurrences(), getDescription());
    }

    @Override
    public String toString() {
        return "TRequirementDefinition{" +
            "capability=" + getCapability() +
            ", node=" + getNode() +
            ", relationship=" + getRelationship() +
            ", occurrences=" + getOccurrences() +
            ", description='" + getDescription() + '\'' +
            '}';
    }

    @NonNull
    public List<String> getOccurrences() {
        if (this.occurrences == null) {
            this.occurrences = new ArrayList<>(Arrays.asList("1", "1"));
        }

        // set default lower bound
        if (this.occurrences.size() < 1) {
            this.occurrences.add("1");
        }

        // set default upper bound
        if (this.occurrences.size() < 2) {
            this.occurrences.add("1");
        }

        return occurrences;
    }

    public void setOccurrences(List<String> occurrences) {
        this.occurrences = occurrences;
    }

    @NonNull
    public QName getCapability() {
        return capability;
    }

    public void setCapability(QName capability) {
        this.capability = capability;
    }

    @Nullable
    public QName getNode() {
        return node;
    }

    public void setNode(QName node) {
        this.node = node;
    }

    @Nullable
    public YTRelationshipDefinition getRelationship() {
        return relationship;
    }

    public void setRelationship(YTRelationshipDefinition relationship) {
        this.relationship = relationship;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NonNull
    public String getUpperBound() {
        return this.getOccurrences().get(1);
    }

    @NonNull
    public Integer getLowerBound() {
        return Integer.valueOf(this.getOccurrences().get(0));
    }

    public <R extends AbstractResult<R>, P extends AbstractParameter<P>> R accept(IVisitor<R, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    public static class Builder {
        private final QName capability;
        private String description;
        private List<String> occurrences;
        private QName node;
        private YTRelationshipDefinition relationship;

        public Builder(QName capability) {
            this.capability = capability;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setOccurrences(List<String> occurrences) {
            this.occurrences = occurrences;
            return this;
        }

        public Builder setOccurrences(int lowerBound, String upperBound) {
            this.occurrences = Stream.of(String.valueOf(lowerBound), upperBound)
                .collect(Collectors.toList());
            return this;
        }

        public Builder setNode(QName node) {
            this.node = node;
            return this;
        }

        public Builder setRelationship(YTRelationshipDefinition relationship) {
            this.relationship = relationship;
            return this;
        }

        public YTRequirementDefinition build() {
            return new YTRequirementDefinition(this);
        }
    }
}
