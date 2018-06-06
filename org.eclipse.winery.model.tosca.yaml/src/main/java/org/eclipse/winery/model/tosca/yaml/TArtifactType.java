/********************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.yaml;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractResult;
import org.eclipse.winery.model.tosca.yaml.visitor.IVisitor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tArtifactType", namespace = " http://docs.oasis-open.org/tosca/ns/simple/yaml/1.0", propOrder = {
    "mimeType",
    "fileExt"
})
public class TArtifactType extends TEntityType {
    @XmlAttribute(name = "mime_type")
    private String mimeType;
    @XmlAttribute(name = "file_ext")
    private List<String> fileExt;

    public TArtifactType() {
    }

    public TArtifactType(Builder builder) {
        super(builder);
        this.setMimeType(builder.mimeType);
        this.setFileExt(builder.fileExt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TArtifactType)) return false;
        if (!super.equals(o)) return false;
        TArtifactType that = (TArtifactType) o;
        return Objects.equals(getMimeType(), that.getMimeType()) &&
            Objects.equals(getFileExt(), that.getFileExt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMimeType(), getFileExt());
    }

    @Override
    public String toString() {
        return "TArtifactType{" +
            "mimeType='" + getMimeType() + '\'' +
            ", fileExt=" + getFileExt() +
            "} " + super.toString();
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @NonNull
    public List<String> getFileExt() {
        if (this.fileExt == null) {
            this.fileExt = new ArrayList<>();
        }

        return fileExt;
    }

    public void setFileExt(List<String> fileExt) {
        this.fileExt = fileExt;
    }

    public <R extends AbstractResult<R>, P extends AbstractParameter<P>> R accept(IVisitor<R, P> visitor, P parameter) {
        R ir1 = super.accept(visitor, parameter);
        R ir2 = visitor.visit(this, parameter);
        if (ir1 == null) {
            return ir2;
        } else {
            return ir1.add(ir2);
        }
    }

    public static class Builder extends TEntityType.Builder<Builder> {
        private String mimeType;
        private List<String> fileExt;

        public Builder() {

        }

        public Builder(TEntityType entityType) {
            super(entityType);
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setFileExt(List<String> fileExt) {
            this.fileExt = fileExt;
            return this;
        }

        public Builder addFileExt(List<String> fileExt) {
            if (fileExt == null || fileExt.isEmpty()) {
                return this;
            }

            if (this.fileExt == null) {
                this.fileExt = new ArrayList<>(fileExt);
            } else {
                this.fileExt.addAll(fileExt);
            }

            return this;
        }

        public Builder addFileExt(String fileExt) {
            if (fileExt == null || fileExt.isEmpty()) {
                return this;
            }

            return addFileExt(Collections.singletonList(fileExt));
        }

        public TArtifactType build() {
            return new TArtifactType(this);
        }
    }
}
