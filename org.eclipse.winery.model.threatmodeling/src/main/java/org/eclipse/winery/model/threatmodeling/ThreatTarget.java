/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.threatmodeling;

import javax.xml.namespace.QName;

public class ThreatTarget {
    private String nodeTemplate;
    private QName nodeType;

    public String getNodeTemplate() {
        return nodeTemplate;
    }

    public void setNodeTemplate(String nodeTemplate) {
        this.nodeTemplate = nodeTemplate;
    }

    public QName getNodeType() {
        return nodeType;
    }

    public void setNodeType(QName nodeType) {
        this.nodeType = nodeType;
    }
}
