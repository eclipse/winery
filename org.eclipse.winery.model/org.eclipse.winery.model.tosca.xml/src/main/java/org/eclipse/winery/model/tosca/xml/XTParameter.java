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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.winery.model.tosca.xml.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tParameter")
public class XTParameter implements Serializable {

    @XmlAttribute(name = "name", required = true)
    @NonNull
    protected String name;

    @XmlAttribute(name = "type", required = true)
    @NonNull
    protected String type;

    @XmlAttribute(name = "required")
    protected XTBoolean required;

    @Deprecated // required for XML deserialization
    public XTParameter() { }

    public XTParameter(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.required = builder.required;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XTParameter)) return false;
        XTParameter that = (XTParameter) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(type, that.type) &&
            required == that.required;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, required);
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String value) {
        Objects.requireNonNull(value);
        this.name = value;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String value) {
        Objects.requireNonNull(value);
        this.type = value;
    }

    /**
     * @return In case the internal model stores <code>null</code>, the default value <code>TBoolean.YES</code> is returned
     */
    @NonNull
    public XTBoolean getRequired() {
        if (required == null) {
            return XTBoolean.YES;
        } else {
            return required;
        }
    }

    public void setRequired(@NonNull XTBoolean value) {
        Objects.requireNonNull(value);
        this.required = value;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public static class Builder {
        private final String name;
        private final String type;
        private final XTBoolean required;

        public Builder(String name, String type, XTBoolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public Builder(String name, String type, Boolean required) {
            this(name, type, required == null ? XTBoolean.YES : required ? XTBoolean.YES : XTBoolean.NO);
        }

        public XTParameter build() {
            return new XTParameter(this);
        }
    }
}
