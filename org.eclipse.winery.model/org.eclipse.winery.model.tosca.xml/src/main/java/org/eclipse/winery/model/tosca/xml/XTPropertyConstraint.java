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

package org.eclipse.winery.model.tosca.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.winery.model.tosca.xml.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPropertyConstraint")
public class XTPropertyConstraint extends XTConstraint {

    @XmlAttribute(name = "property", required = true)
    protected String property;

    @Deprecated // required for XML deserialization
    public XTPropertyConstraint() {
        super();
    }

    public XTPropertyConstraint(Builder builder) {
        super(builder);
        this.property = builder.property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XTPropertyConstraint)) return false;
        if (!super.equals(o)) return false;
        XTPropertyConstraint that = (XTPropertyConstraint) o;
        return Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), property);
    }

    @NonNull
    public String getProperty() {
        return property;
    }

    public void setProperty(@NonNull String value) {
        this.property = value;
    }

    public static class Builder extends XTConstraint.Builder<Builder> {
        private String property;

        public Builder setProperty(String property) {
            this.property = property;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        public XTPropertyConstraint build() {
            return new XTPropertyConstraint(this);
        }
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
