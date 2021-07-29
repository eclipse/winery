/*******************************************************************************
 * Copyright (c) 2013-2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.tosca.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.xml.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCapabilityDefinition", propOrder = {
    "constraints"
})
public class XTCapabilityDefinition extends XTExtensibleElements {

    @XmlElementWrapper(name = "Constraints")
    @XmlElement(name = "Constraint", required = true)
    protected List<XTConstraint> constraints;

    @XmlAttribute(name = "name", required = true)
    protected String name;

    @XmlAttribute(name = "capabilityType", required = true)
    protected QName capabilityType;

    @XmlAttribute(name = "lowerBound")
    protected Integer lowerBound;

    @XmlAttribute(name = "upperBound")
    protected String upperBound;

    @XmlAttribute(name = "validSourceTypes")
    protected List<QName> validSourceTypes;

    @Deprecated // required for XML deserialization
    public XTCapabilityDefinition() {
    }

    public XTCapabilityDefinition(Builder builder) {
        super(builder);
        this.constraints = builder.constraints;
        this.name = builder.name;
        this.capabilityType = builder.capabilityType;
        this.lowerBound = builder.lowerBound;
        this.upperBound = builder.upperBound;
        this.validSourceTypes = builder.validSourceTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XTCapabilityDefinition)) return false;
        XTCapabilityDefinition that = (XTCapabilityDefinition) o;
        return Objects.equals(constraints, that.constraints) &&
            Objects.equals(name, that.name) &&
            Objects.equals(capabilityType, that.capabilityType) &&
            Objects.equals(lowerBound, that.lowerBound) &&
            Objects.equals(upperBound, that.upperBound) &&
            Objects.equals(validSourceTypes, that.validSourceTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraints, name, capabilityType, lowerBound, upperBound, validSourceTypes);
    }

    public List<XTConstraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<XTConstraint> value) {
        this.constraints = value;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String value) {
        Objects.requireNonNull(value);
        this.name = value;
    }

    @Nullable
    public QName getCapabilityType() {
        return capabilityType;
    }

    public void setCapabilityType(@Nullable QName value) {
        this.capabilityType = value;
    }

    public int getLowerBound() {
        if (lowerBound == null) {
            return 1;
        } else {
            return lowerBound;
        }
    }

    public void setLowerBound(@Nullable Integer value) {
        this.lowerBound = value;
    }

    @NonNull
    public String getUpperBound() {
        if (upperBound == null) {
            return "1";
        } else {
            return upperBound;
        }
    }

    public void setUpperBound(@Nullable String value) {
        this.upperBound = value;
    }

    public List<QName> getValidSourceTypes() {
        return validSourceTypes;
    }

    public void setValidSourceTypes(List<QName> value) {
        this.validSourceTypes = value;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public static class Builder extends XTExtensibleElements.Builder<Builder> {
        private final String name;
        private final QName capabilityType;

        private List<XTConstraint> constraints;
        private Integer lowerBound;
        private String upperBound;

        private List<QName> validSourceTypes;

        public Builder(String name, QName capabilityType) {
            this.name = name;
            this.capabilityType = capabilityType;
        }

        public Builder setConstraints(List<XTConstraint> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder setLowerBound(Integer lowerBound) {
            this.lowerBound = lowerBound;
            return this;
        }

        public Builder setUpperBound(String upperBound) {
            this.upperBound = upperBound;
            return this;
        }

        public Builder setValidSourceTypes(List<QName> validSourceTypes) {
            this.validSourceTypes = validSourceTypes;
            return this;
        }

        public Builder addConstraints(List<XTConstraint> constraints) {
            if (constraints == null || constraints.isEmpty()) {
                return this;
            }

            if (this.constraints == null) {
                this.constraints = constraints;
            } else {
                this.constraints.addAll(constraints);
            }
            return this;
        }

        public Builder addConstraint(XTConstraint constraints) {
            if (constraints == null) {
                return this;
            }

            List<XTConstraint> tmp = new ArrayList<>();
            tmp.add(constraints);
            return addConstraints(tmp);
        }

        @Override
        public Builder self() {
            return this;
        }

        public XTCapabilityDefinition build() {
            return new XTCapabilityDefinition(this);
        }
    }
}
