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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.jdt.annotation.NonNull;

/**
 * <p>Java class for tInterface complex type.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;complexType name="tInterface">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Operation" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tOperation"
 * maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tInterface", propOrder = {
    "operations"
})
public class TInterface implements HasName, Serializable {

    @XmlElement(name = "Operation", required = true)
    protected List<TOperation> operations;

    @XmlAttribute(name = "name", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String name;

    @Deprecated // used for XML deserialization of API request content
    public TInterface() {
    }

    public TInterface(Builder builder) {
        this.operations = builder.operation;
        this.name = builder.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TInterface)) return false;
        TInterface that = (TInterface) o;
        return Objects.equals(operations, that.operations) &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operations, name);
    }

    /**
     * Gets the value of the operation property.
     * <p>
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore, any modification you make
     * to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method
     * for the operation property.
     * <p>
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOperation().add(newItem);
     * </pre>
     * <p>
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list {@link TOperation }
     */
    @NonNull
    public List<TOperation> getOperations() {
        if (operations == null) {
            operations = new ArrayList<>();
        }
        return this.operations;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     */
    @NonNull
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is {@link String }
     */
    @Override
    public void setName(String value) {
        this.name = value;
    }

    public static class Builder {
        private final String name;
        private final List<TOperation> operation;

        public Builder(String name, List<TOperation> operation) {
            this.name = name;
            this.operation = operation;
        }

        public TInterface build() {
            return new TInterface(this);
        }
    }
}
