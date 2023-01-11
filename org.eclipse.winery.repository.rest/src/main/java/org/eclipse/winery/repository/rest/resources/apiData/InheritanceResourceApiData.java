/*******************************************************************************
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

package org.eclipse.winery.repository.rest.resources.apiData;

import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResourceWithNameDerivedFromAbstractFinal;

public class InheritanceResourceApiData {

    public String isAbstract;
    public String isFinal;
    public String derivedFrom;
    public boolean cyclicInheritance;

    public InheritanceResourceApiData() {
    }

    public InheritanceResourceApiData(AbstractComponentInstanceResourceWithNameDerivedFromAbstractFinal res) {
        this.isAbstract = res.getTBoolean("getAbstract");
        this.isFinal = res.getTBoolean("getFinal");
        this.derivedFrom = res.getDerivedFrom() == null ? "(none)" : res.getDerivedFrom();
        this.cyclicInheritance = res.hasCyclicDependency();
    }

    public String toString() {
        return "InheritanceResourceJson: { isAbstract: " + this.isAbstract + ", isFinal: " + this.isFinal + ", derivedFrom: " + this.derivedFrom + ", cyclicInheritance: " + this.cyclicInheritance + " }";
    }
}
