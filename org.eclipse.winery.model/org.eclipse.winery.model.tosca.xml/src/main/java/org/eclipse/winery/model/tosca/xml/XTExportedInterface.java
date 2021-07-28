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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.winery.model.tosca.xml.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tExportedInterface", propOrder = {
    "operation"
})
public class XTExportedInterface implements XHasName, Serializable {

    @XmlElement(name = "Operation", required = true)
    protected List<XTExportedOperation> operation;

    @XmlAttribute(name = "name", required = true)
    @XmlSchemaType(name = "anyURI")
    @NonNull
    protected String name;

    @Deprecated // required for XML deserialization
    public XTExportedInterface() {
    }

    public XTExportedInterface(Builder builder) {
        this.name = builder.name;
        this.operation = builder.operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XTExportedInterface)) return false;
        XTExportedInterface that = (XTExportedInterface) o;
        return Objects.equals(operation, that.operation) &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, name);
    }

    @NonNull
    public List<XTExportedOperation> getOperation() {
        if (operation == null) {
            operation = new ArrayList<>();
        }
        return this.operation;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NonNull String value) {
        Objects.requireNonNull(value);
        this.name = value;
    }

    public static class Builder {

        private final List<XTExportedOperation> operation;
        private final String name;

        public Builder(String name, List<XTExportedOperation> operation) {
            this.operation = operation;
            this.name = name;
        }

        public XTExportedInterface build() {
            return new XTExportedInterface(this);
        }
    }

    public void accept(Visitor visitor) {
        visitor.accept(this);
    }
}
