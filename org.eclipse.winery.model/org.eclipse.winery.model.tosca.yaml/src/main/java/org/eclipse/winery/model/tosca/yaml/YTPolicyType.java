/********************************************************************************
 * Copyright (c) 2017-2021 Contributors to the Eclipse Foundation
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractResult;
import org.eclipse.winery.model.tosca.yaml.visitor.IVisitor;

import org.eclipse.jdt.annotation.NonNull;

public class YTPolicyType extends YTEntityType {
    private List<QName> targets;
    private Map<String, YTTriggerDefinition> triggers;

    protected YTPolicyType(Builder builder) {
        super(builder);
        this.setTargets(builder.targets);
        this.setTriggers(builder.triggers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YTPolicyType)) return false;
        if (!super.equals(o)) return false;
        YTPolicyType that = (YTPolicyType) o;
        return Objects.equals(getTargets(), that.getTargets()) &&
            Objects.equals(getTriggers(), that.getTriggers());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTargets(), getTriggers());
    }

    @Override
    public String toString() {
        return "TPolicyType{" +
            "targets=" + getTargets() +
            ", triggers=" + getTriggers() +
            "} " + super.toString();
    }

    @NonNull
    public List<QName> getTargets() {
        if (this.targets == null) {
            this.targets = new ArrayList<>();
        }

        return targets;
    }

    public void setTargets(List<QName> targets) {
        this.targets = targets;
    }

    @NonNull
    public Map<String, YTTriggerDefinition> getTriggers() {
        if (this.triggers == null) {
            this.triggers = new LinkedHashMap<>();
        }

        return triggers;
    }

    public void setTriggers(Map<String, YTTriggerDefinition> triggers) {
        this.triggers = triggers;
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

        private List<QName> targets;
        private Map<String, YTTriggerDefinition> triggers;

        public Builder() {
        }

        public Builder(YTEntityType entityType) {
            super(entityType);
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder setTargets(List<QName> targets) {
            this.targets = targets;
            return this;
        }

        public Builder setTriggers(Map<String, YTTriggerDefinition> triggers) {
            this.triggers = triggers;
            return this;
        }

        public Builder addTargets(List<QName> targets) {
            if (targets == null || targets.isEmpty()) {
                return this;
            }

            if (this.targets == null) {
                this.targets = new ArrayList<>(targets);
            } else {
                this.targets.addAll(targets);
            }

            return this;
        }

        public Builder addTargets(QName target) {
            if (target == null) {
                return this;
            }

            return addTargets(Collections.singletonList(target));
        }

        public YTPolicyType build() {
            return new YTPolicyType(this);
        }
    }
}
