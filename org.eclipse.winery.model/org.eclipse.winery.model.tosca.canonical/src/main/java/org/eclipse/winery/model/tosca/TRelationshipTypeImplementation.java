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

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;

@XmlAccessorType(XmlAccessType.FIELD)
// by using @XmlTransient at TEntityTypeImplementation, this orders *all* elements, even if IntelliJ marks them in red
// see https://stackoverflow.com/a/6790388/873282
@XmlType(name = "tRelationshipTypeImplementation", propOrder = {
    "tags",
    "derivedFrom",
    "requiredContainerFeatures",
    "implementationArtifacts"
})
public class TRelationshipTypeImplementation extends TEntityTypeImplementation {

    @XmlElement(name = "DerivedFrom")
    protected TRelationshipTypeImplementation.DerivedFrom derivedFrom;

    @Deprecated // used for XML deserialization of API request content
    public TRelationshipTypeImplementation() { }

    public TRelationshipTypeImplementation(Builder builder) {
        super(builder);
        this.derivedFrom = builder.derivedFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TRelationshipTypeImplementation)) return false;
        if (!super.equals(o)) return false;
        TRelationshipTypeImplementation that = (TRelationshipTypeImplementation) o;
        return Objects.equals(derivedFrom, that.derivedFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), derivedFrom);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    //@Nullable
    public TRelationshipTypeImplementation.DerivedFrom getDerivedFrom() {
        return derivedFrom;
    }

    public void setDerivedFrom(HasType value) {
        this.derivedFrom = (TRelationshipTypeImplementation.DerivedFrom) value;
    }

    @NonNull
    @XmlAttribute(name = "relationshipType", required = true)
    public QName getRelationshipType() {
        return this.implementedType;
    }

    public void setRelationshipType(QName value) {
        this.implementedType = value;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DerivedFrom implements HasType, Serializable {

        @XmlAttribute(name = "relationshipTypeImplementationRef", required = true)
        protected QName relationshipTypeImplementationRef;

        @NonNull
        public QName getRelationshipTypeImplementationRef() {
            return relationshipTypeImplementationRef;
        }

        public void setRelationshipTypeImplementationRef(QName value) {
            this.relationshipTypeImplementationRef = value;
        }

        public QName getType() {
            return this.getRelationshipTypeImplementationRef();
        }

        @Override
        public void setType(QName type) {
            this.setRelationshipTypeImplementationRef(type);
        }

        @Override
        public QName getTypeAsQName() {
            return this.getType();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DerivedFrom that = (DerivedFrom) o;
            return Objects.equals(relationshipTypeImplementationRef, that.relationshipTypeImplementationRef);
        }

        @Override
        public int hashCode() {
            return Objects.hash(relationshipTypeImplementationRef);
        }
    }

    public static class Builder extends TEntityTypeImplementation.Builder<Builder> {

        private TRelationshipTypeImplementation.DerivedFrom derivedFrom;

        public Builder(Builder builder, String name, QName implementedType) {
            super(builder, name, implementedType);
        }

        public Builder(String name, QName implementedRelationshipType) {
            super(name, implementedRelationshipType);
        }

        public Builder setDerivedFrom(DerivedFrom derivedFrom) {
            this.derivedFrom = derivedFrom;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public TRelationshipTypeImplementation build() {
            return new TRelationshipTypeImplementation(this);
        }
    }
}
