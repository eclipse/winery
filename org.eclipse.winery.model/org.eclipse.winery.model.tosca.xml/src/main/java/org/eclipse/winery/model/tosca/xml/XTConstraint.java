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

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tConstraint", propOrder = {
    "any"
})
@XmlSeeAlso( {
    XTPropertyConstraint.class
})
public class XTConstraint implements Serializable {

    @XmlAnyElement(lax = true)
    protected Object any;
    @XmlAttribute(name = "constraintType", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String constraintType;

    @Deprecated // required for XML deserialization
    public XTConstraint() {
    }

    protected XTConstraint(Builder<?> builder) {
        this.any = builder.any;
        this.constraintType = builder.constraintType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XTConstraint)) return false;
        XTConstraint that = (XTConstraint) o;
        return Objects.equals(any, that.any) &&
            Objects.equals(constraintType, that.constraintType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(any, constraintType);
    }

    @Nullable
    public Object getAny() {
        return any;
    }

    public void setAny(@Nullable Object value) {
        this.any = value;
    }

    @NonNull
    public String getConstraintType() {
        return constraintType;
    }

    public void setConstraintType(String value) {
        this.constraintType = Objects.requireNonNull(value);
    }

    public static class Builder<T extends Builder<T>> {
        private Object any;
        private String constraintType;

        public T setAny(Object any) {
            this.any = any;
            return self();
        }

        public T setConstraintType(String constraintType) {
            this.constraintType = constraintType;
            return self();
        }

        @SuppressWarnings("unchecked")
        public T self() {
            return (T) this;
        }

        public XTConstraint build() {
            return new XTConstraint(this);
        }
    }
}
