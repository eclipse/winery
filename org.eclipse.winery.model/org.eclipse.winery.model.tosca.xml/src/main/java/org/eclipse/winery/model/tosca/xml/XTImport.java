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

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.winery.model.tosca.xml.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tImport")
public class XTImport extends XTExtensibleElements {
    @XmlAttribute(name = "namespace")
    @XmlSchemaType(name = "anyURI")
    protected String namespace;
    @XmlAttribute(name = "location")
    @XmlSchemaType(name = "anyURI")
    protected String location;
    @XmlAttribute(name = "importType", required = true)
    protected String importType;

    @Deprecated // required for XML deserialization
    public XTImport() { }

    public XTImport(Builder builder) {
        super(builder);
        this.namespace = builder.namespace;
        this.location = builder.location;
        this.importType = builder.importType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XTImport)) return false;
        if (!super.equals(o)) return false;
        XTImport tImport = (XTImport) o;
        return Objects.equals(namespace, tImport.namespace) &&
            Objects.equals(location, tImport.location) &&
            Objects.equals(importType, tImport.importType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), namespace, location, importType);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(@Nullable String value) {
        this.namespace = value;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String value) {
        this.location = value;
    }

    @NonNull
    public String getImportType() {
        return importType;
    }

    public void setImportType(@NonNull String value) {
        Objects.requireNonNull(value);
        this.importType = value;
    }

    public static class Builder extends XTExtensibleElements.Builder<Builder> {
        private final String importType;
        private String namespace;
        private String location;

        public Builder(String importType) {
            this.importType = importType;
        }

        public Builder(XTExtensibleElements extensibleElements, String importType) {
            super(extensibleElements);
            this.importType = importType;
        }

        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder setLocation(String location) {
            this.location = location;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public XTImport build() {
            return new XTImport(this);
        }
    }
}
