/*******************************************************************************
 * Copyright (c) 2013 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v20.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Oliver Kopp - initial API and implementation
 *******************************************************************************/
package org.eclipse.winery.common.ids.definitions;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.Namespace;
import org.eclipse.winery.common.ids.XmlId;

/**
 * An instance of this class is either a NodeTypeId or a RelationShipTypeId
 */
public abstract class TopologyGraphElementEntityTypeId extends EntityTypeId {

	public TopologyGraphElementEntityTypeId(Namespace namespace, XmlId xmlId) {
		super(namespace, xmlId);
	}

	public TopologyGraphElementEntityTypeId(String ns, String id, boolean URLencoded) {
		super(ns, id, URLencoded);
	}

	public TopologyGraphElementEntityTypeId(QName type) {
		super(type);
	}

}
