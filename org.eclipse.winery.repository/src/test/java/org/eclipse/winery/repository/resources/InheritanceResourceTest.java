/**
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Lukas Harzenetter - initial API and implementation
 */

package org.eclipse.winery.repository.resources;

import org.junit.Test;

public class InheritanceResourceTest extends AbstractResourceTest {

	@Test
	public void addInheritanceToNodeType() throws Exception {
		this.setRevisionTo("2fd9edf31bc0d7a1118fa19eb7050922d0653cb0");
		this.assertPut("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/grape/inheritance/", "entitytypes/nodetypes/grape_add_inheritance.json");
		this.assertGet("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/grape/inheritance/", "entitytypes/nodetypes/grape_add_inheritance.json");
		// Also assure that the XML is still valid
		this.assertGet("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/grape/", "entitytypes/nodetypes/grape_inheritance.xml");
	}

	@Test
	public void xmlStillValidAfterAddingDerivedFromNoneInheritanceToOneNodeType() throws Exception {
		this.setRevisionTo("2fd9edf31bc0d7a1118fa19eb7050922d0653cb0");
		this.assertPut("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/grape/inheritance/", "entitytypes/nodetypes/grape_add_none_inheritance.json");
		this.assertGet("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/grape/", "entitytypes/nodetypes/grape_inheritance_none.xml");
	}

	@Test
	public void addInheritanceToPolicyType() throws Exception {
		this.setRevisionTo("96a908b37fd3ee190d6371eff4112455eb0097fb");
		this.assertPut("policytypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fpolicytypes%252Ffruits/european/inheritance/", "entitytypes/policytypes/european_add_inheritance.json");
		this.assertGet("policytypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fpolicytypes%252Ffruits/european/inheritance/", "entitytypes/policytypes/european_add_inheritance.json");
		// Also assure that the XML is still valid
		this.assertGet("policytypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fpolicytypes%252Ffruits/european/", "entitytypes/policytypes/european_inheritance.xml");
	}

	@Test
	public void xmlStillValidAfterAddingDerivedFromNoneInheritanceToOnePolicyType() throws Exception {
		this.setRevisionTo("96a908b37fd3ee190d6371eff4112455eb0097fb");
		this.assertPut("policytypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fpolicytypes%252Ffruits/european/inheritance/", "entitytypes/policytypes/organic_add_none_inheritance.json");
		this.assertGet("policytypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fpolicytypes%252Ffruits/european/", "entitytypes/policytypes/organic_inheritance_none.xml");
	}
}
