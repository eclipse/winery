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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "Definitions")
public class XDefinitions extends XTDefinitions {

    @XmlTransient
    protected Map<String, QName> importDefinitions = new HashMap<>();

    @Deprecated // required for XML deserialization
    public XDefinitions() {
    }

    public XDefinitions(Builder builder) {
        super(builder);
    }

    public Map<String, QName> getImportDefinitions() {
        return importDefinitions;
    }

    public void setImportDefinitions(Map<String, QName> importDefinitions) {
        this.importDefinitions = importDefinitions;
    }

    public static class Builder extends XTDefinitions.Builder<Builder> {
        public Builder(String id, String target_namespace) {
            super(id, target_namespace);
        }

        @Override
        public Builder self() {
            return this;
        }

        public XDefinitions build() {
            return new XDefinitions(this);
        }
    }
}
