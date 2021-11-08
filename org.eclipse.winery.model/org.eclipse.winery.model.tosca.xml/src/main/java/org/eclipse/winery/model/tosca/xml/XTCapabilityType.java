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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.xml.visitor.Visitor;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tCapabilityType")
public class XTCapabilityType extends XTEntityType {
    // this is added here to support the YAML model
    // cannot be annotated as transient, otherwise it will be dropped. Reason: org.eclipse.winery.repository.backend.BackendUtils.persist(
    // java.lang.Object, org.eclipse.winery.common.RepositoryFileReference, org.apache.tika.mime.MediaType)
    private List<QName> validNodeTypes;

    @Deprecated // required for XML deserialization
    public XTCapabilityType() { }

    public XTCapabilityType(Builder builder) {
        super(builder);
        this.validNodeTypes = builder.validSourceTypes;
    }

    public List<QName> getValidNodeTypes() {
        return validNodeTypes;
    }

    public void setValidNodeTypes(List<QName> validNodeTypes) {
        this.validNodeTypes = validNodeTypes;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public static class Builder extends XTEntityType.Builder<Builder> {
        private List<QName> validSourceTypes;

        public Builder(String name) {
            super(name);
        }

        public Builder(XTCapabilityType entityType) {
            super(entityType);
            this.validSourceTypes = entityType.validNodeTypes;
        }

        public Builder setValidSourceTypes(List<QName> validSourceTypes) {
            this.validSourceTypes = validSourceTypes;
            return self();
        }

        @Override
        public Builder self() {
            return this;
        }

        public XTCapabilityType build() {
            return new XTCapabilityType(this);
        }
    }
}
