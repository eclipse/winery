/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public abstract class TDeploymentOrImplementationArtifact extends TExtensibleElements implements HasName, Serializable {

    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "artifactType", required = true)
    protected QName artifactType;
    @XmlAttribute(name = "artifactRef")
    protected QName artifactRef;

    @Deprecated // used for XML deserialization of API request content
    public TDeploymentOrImplementationArtifact() {
    }

    public TDeploymentOrImplementationArtifact(TDeploymentOrImplementationArtifact.Builder<?> builder) {
        super(builder);
        this.name = builder.name;
        this.artifactType = builder.artifactType;
        this.artifactRef = builder.artifactRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TDeploymentArtifact)) return false;
        TDeploymentArtifact that = (TDeploymentArtifact) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(artifactType, that.artifactType) &&
            Objects.equals(artifactRef, that.artifactRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, artifactType, artifactRef);
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NonNull String value) {
        this.name = Objects.requireNonNull(value);
    }

    // apparently this can be null, even though it shouldn't ever be...
    @Nullable
    public QName getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(@NonNull QName value) {
        this.artifactType = Objects.requireNonNull(value);
    }

    @Nullable
    public QName getArtifactRef() {
        return artifactRef;
    }

    public void setArtifactRef(@Nullable QName value) {
        this.artifactRef = value;
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public static class Builder<T extends Builder<T>> extends TExtensibleElements.Builder<T> {

        protected final QName artifactType;
        protected String name;
        protected QName artifactRef;

        public Builder(QName artifactType) {
            this.artifactType = artifactType;
        }

        public T setArtifactRef(QName artifactRef) {
            this.artifactRef = artifactRef;
            return self();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T self() {
            return (T) this;
        }
    }
}
