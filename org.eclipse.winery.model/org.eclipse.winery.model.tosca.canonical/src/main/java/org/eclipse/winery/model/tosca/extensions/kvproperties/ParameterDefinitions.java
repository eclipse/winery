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
package org.eclipse.winery.model.tosca.extensions.kvproperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ParameterDefinitions")
@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterDefinitions implements Serializable {

    @XmlElement(name = "ParameterDefinition")
    private List<ParameterDefinition> parameterDefinition;

    public ParameterDefinitions() {
    }

    public ParameterDefinitions(List<ParameterDefinition> parameterDefinition) {
        this.parameterDefinition = parameterDefinition;
    }

    public List<ParameterDefinition> getParameterDefinition() {
        if (this.parameterDefinition == null) {
            this.parameterDefinition = new ArrayList<>();
        }
        return parameterDefinition;
    }
}