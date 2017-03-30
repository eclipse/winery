/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Oliver Kopp - initial API and implementation
 *     Nicole Keppler - JSON test
 *******************************************************************************/
package org.eclipse.winery.repository.resources.entitytypes.nodetypes;

import org.eclipse.winery.repository.Utils;
import org.eclipse.winery.repository.resources.AbstractResourceTest;
import org.eclipse.winery.repository.resources.TestIds;

import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

public class NodeTypeResourceTest extends AbstractResourceTest {

	@Test
	public void baboabInitialExistsUsingResource() throws Exception {
		this.setRevisionTo("5b5ad1106a3a428020b6bc5d2f154841acb5f779"); // repository containing boabab fruit only
		NodeTypeResource nodeTypeResource = (NodeTypeResource) NodeTypesResource.getComponentInstaceResource(TestIds.ID_FRUIT_BAOBAB);
		String testXml = Utils.getXMLAsString(nodeTypeResource.getNodeType());
		String controlXml = this.readFromClasspath("entitytypes/nodetypes/baobab_initial.xml");
		org.hamcrest.MatcherAssert.assertThat(testXml, CompareMatcher.isIdenticalTo(controlXml).ignoreWhitespace());
	}

	@Test
	public void baboabInitialExistsUsingRest() throws Exception {
		this.setRevisionTo("5b5ad1106a3a428020b6bc5d2f154841acb5f779");
		this.assertGet("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/", "entitytypes/nodetypes/baobab_initial_with_definitions.xml");
	}

	@Test
	public void baobabCapabilitiesJSON() throws Exception {
		this.setRevisionTo("8b125a426721f8a0eb17340dc08e9b571b0cd7f7");
		this.assertGet("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/", "entitytypes/nodetypes/baobab_capabilites.json");
	}

	@Test
	public void baobabRoundTrip() throws Exception {
		this.setRevisionTo("15cd64e30770ca7986660a34e1a4a7e0cf332f19"); // empty repository
		this.assertNotFound("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/");

		// this is not possible, we have to create it first (post) and then put data
		// this.assertPost("nodetypes/", "entitytypes/nodetypes/baobab_initial.xml");
		this.assertPost("nodetypes/", "http://winery.opentosca.org/test/nodetypes/fruits", "baobab");
		this.assertPut("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/", "entitytypes/nodetypes/baobab_initial_with_definitions_put.xml");

		this.assertGet("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/", "entitytypes/nodetypes/baobab_initial_with_definitions_expected.xml");

		this.assertPut("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/", "entitytypes/nodetypes/baobab_updated_put.xml");
		this.assertGet("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/", "entitytypes/nodetypes/baobab_updated_expected.xml");
		this.assertDelete("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/");
		this.assertNotFound("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/");
	}

	@Test
	public void baboabHasNoImplementations() throws Exception {
		this.setRevisionTo("5b5ad1106a3a428020b6bc5d2f154841acb5f779");
		this.assertGetSize("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/implementations", 0);
	}

	@Test
	public void baboabHasNoImage() throws Exception {
		this.setRevisionTo("5b5ad1106a3a428020b6bc5d2f154841acb5f779");
		start()
				.get(callURL("nodetypes/http%253A%252F%252Fwinery.opentosca.org%252Ftest%252Fnodetypes%252Ffruits/baobab/visualappearance/50x50"))
				.then()
				.statusCode(404);
	}

}
