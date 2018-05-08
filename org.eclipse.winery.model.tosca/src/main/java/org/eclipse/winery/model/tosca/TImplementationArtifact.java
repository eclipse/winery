/*******************************************************************************
 * Copyright (c) 2013-2018 Contributors to the Eclipse Foundation
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

import io.github.adr.embedded.ADR;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.Objects;

/**
 * <p>Java class for tImplementationArtifact complex type.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;complexType name="tImplementationArtifact">
 *   &lt;complexContent>
 *     &lt;extension base="{http://docs.oasis-open.org/tosca/ns/2011/12}tExtensibleElements">
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="interfaceName" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="operationName" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       &lt;attribute name="artifactType" use="required" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;attribute name="artifactRef" type="{http://www.w3.org/2001/XMLSchema}QName" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tImplementationArtifact")
@XmlSeeAlso( {
    org.eclipse.winery.model.tosca.TImplementationArtifacts.ImplementationArtifact.class
})
public class TImplementationArtifact extends TExtensibleElements implements HasName {

    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "interfaceName")
    @XmlSchemaType(name = "anyURI")
    protected String interfaceName;
    @XmlAttribute(name = "operationName")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String operationName;
    @XmlAttribute(name = "artifactType", required = true)
    protected QName artifactType;
    @XmlAttribute(name = "artifactRef")
    protected QName artifactRef;

    public TImplementationArtifact() {

    }

    public TImplementationArtifact(Builder builder) {
        super(builder);
        this.name = builder.name;
        this.interfaceName = builder.interfaceName;
        this.operationName = builder.operationName;
        this.artifactType = builder.artifactType;
        this.artifactRef = builder.artifactRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TImplementationArtifact)) return false;
        if (!super.equals(o)) return false;
        TImplementationArtifact that = (TImplementationArtifact) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(interfaceName, that.interfaceName) &&
            Objects.equals(operationName, that.operationName) &&
            Objects.equals(artifactType, that.artifactType) &&
            Objects.equals(artifactRef, that.artifactRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, interfaceName, operationName, artifactType, artifactRef);
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     */
    @Nullable
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

    /**
     * Gets the value of the interfaceName property.
     *
     * @return possible object is {@link String }
     */
    @Nullable
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * Sets the value of the interfaceName property.
     *
     * @param value allowed object is {@link String }
     */
    public void setInterfaceName(String value) {
        this.interfaceName = value;
    }

    /**
     * Gets the value of the operationName property.
     *
     * @return possible object is {@link String }
     */
    @Nullable
    public String getOperationName() {
        return operationName;
    }

    /**
     * Sets the value of the operationName property.
     *
     * @param value allowed object is {@link String }
     */
    public void setOperationName(String value) {
        this.operationName = value;
    }

    /**
     * Gets the value of the artifactType property.
     *
     * @return possible object is {@link QName }
     */
    @NonNull
    public QName getArtifactType() {
        return artifactType;
    }

    /**
     * Sets the value of the artifactType property.
     *
     * @param value allowed object is {@link QName }
     */
    public void setArtifactType(QName value) {
        this.artifactType = value;
    }

    /**
     * Gets the value of the artifactRef property.
     *
     * @return possible object is {@link QName }
     */
    @Nullable
    public QName getArtifactRef() {
        return artifactRef;
    }

    /**
     * Sets the value of the artifactRef property.
     *
     * @param value allowed object is {@link QName }
     */
    public void setArtifactRef(QName value) {
        this.artifactRef = value;
    }

    public static class Builder<T extends Builder<T>> extends TExtensibleElements.Builder<Builder<T>> {
        private final QName artifactType;

        private String name;
        private String interfaceName;
        private String operationName;
        private QName artifactRef;

        public Builder(QName artifactType) {
            this.artifactType = artifactType;
        }

        public T setName(String name) {
            this.name = name;
            return self();
        }

        public T setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return self();
        }

        public T setOperationName(String operationName) {
            this.operationName = operationName;
            return self();
        }

        public T setArtifactRef(QName artifactRef) {
            this.artifactRef = artifactRef;
            return self();
        }

        @ADR(11)
        @Override
        public T self() {
            return (T) this;
        }

        public TImplementationArtifact build() {
            return new TImplementationArtifact(this);
        }
    }
}
