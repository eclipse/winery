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

import java.util.List;
import java.util.Objects;

import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractResult;
import org.eclipse.winery.model.tosca.yaml.visitor.IVisitor;
import org.eclipse.winery.model.tosca.yaml.visitor.VisitorNode;

public class YTConstraintClause implements VisitorNode {
    
    private String key;
    private String value;
    private List<String> list;

    protected YTConstraintClause(Builder builder) {
        this.key = builder.key;
        this.value = builder.value;
        this.list = builder.list;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "TConstraintClause{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            ", list=" + list +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YTConstraintClause that = (YTConstraintClause) o;
        return Objects.equals(key, that.key) &&
            Objects.equals(value, that.value) &&
            Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, list);
    }

    public static class Builder {
        private String key;
        private String value;
        private List<String> list;

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public Builder setList(List<String> list) {
            this.list = list;
            return this;
        }
        
        public YTConstraintClause build() {
            return new YTConstraintClause(this);
        }
    } 
    
    public <R extends AbstractResult<R>, P extends AbstractParameter<P>> R accept(IVisitor<R, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }
}
