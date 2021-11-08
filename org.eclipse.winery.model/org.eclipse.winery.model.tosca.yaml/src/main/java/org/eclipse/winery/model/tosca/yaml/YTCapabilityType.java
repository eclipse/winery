/********************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractResult;
import org.eclipse.winery.model.tosca.yaml.visitor.IVisitor;

import org.eclipse.jdt.annotation.NonNull;

public class YTCapabilityType extends YTEntityType {
    private List<QName> validSourceTypes;

    protected YTCapabilityType(Builder builder) {
        super(builder);
        this.setValidSourceTypes(builder.validSourceTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YTCapabilityType)) return false;
        if (!super.equals(o)) return false;
        YTCapabilityType that = (YTCapabilityType) o;
        return Objects.equals(getValidSourceTypes(), that.getValidSourceTypes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getValidSourceTypes());
    }

    @Override
    public String toString() {
        return "TCapabilityType{" +
            "validSourceTypes=" + getValidSourceTypes() +
            "} " + super.toString();
    }

    @NonNull
    public List<QName> getValidSourceTypes() {
        if (this.validSourceTypes == null) {
            this.validSourceTypes = new ArrayList<>();
        }

        return validSourceTypes;
    }

    public void setValidSourceTypes(List<QName> validSourceTypes) {
        this.validSourceTypes = validSourceTypes;
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

    public static class Builder extends YTEntityType.Builder<Builder> {
        private List<QName> validSourceTypes;

        public Builder() {

        }

        public Builder(YTEntityType entityType) {
            super(entityType);
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder setValidSourceTypes(List<QName> validSourceTypes) {
            this.validSourceTypes = validSourceTypes;
            return this;
        }

        public Builder addValidSourceTypes(List<QName> validSourceTypes) {
            if (validSourceTypes == null || validSourceTypes.isEmpty()) {
                return this;
            }

            if (this.validSourceTypes == null) {
                this.validSourceTypes = new ArrayList<>(validSourceTypes);
            } else {
                this.validSourceTypes.addAll(validSourceTypes);
            }

            return this;
        }

        public Builder addValidSourceTypes(QName validSourceType) {
            if (validSourceType == null) {
                return this;
            }

            return addValidSourceTypes(Collections.singletonList(validSourceType));
        }

        public YTCapabilityType build() {
            return new YTCapabilityType(this);
        }
    }
}
