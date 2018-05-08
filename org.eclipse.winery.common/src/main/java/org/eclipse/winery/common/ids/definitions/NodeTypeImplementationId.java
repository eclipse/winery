/*******************************************************************************
 * Copyright (c) 2013-2018 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.common.ids.definitions;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.Namespace;
import org.eclipse.winery.common.ids.XmlId;

public final class NodeTypeImplementationId extends EntityTypeImplementationId {

    public NodeTypeImplementationId(Namespace namespace, XmlId xmlId) {
        super(namespace, xmlId);
    }

    public NodeTypeImplementationId(String ns, String id, boolean URLencoded) {
        super(ns, id, URLencoded);
    }

    public NodeTypeImplementationId(QName type) {
        super(type);
    }

    public String getGroup() {
        return "NodeTypeImplementation";
    }
}
