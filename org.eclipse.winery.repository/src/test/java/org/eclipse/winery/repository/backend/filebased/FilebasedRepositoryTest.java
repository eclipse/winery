/**
 * Copyright (c) 2017 University of Stuttgart. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 and the Apache License 2.0 which both accompany this
 * distribution, and are available at http://www.eclipse.org/legal/epl-v20.html and
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *  - Lukas Harzenetter - initial API and implementation
 *  - Oliver Kopp - test for namespaceWithSpaceAtEndWorks
 */
package org.eclipse.winery.repository.backend.filebased;

import java.util.SortedSet;

import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.imports.XSDImportId;
import org.eclipse.winery.repository.TestWithGitBackedRepository;

import org.junit.Assert;
import org.junit.Test;

public class FilebasedRepositoryTest extends TestWithGitBackedRepository {

	@Test
	public void getAllDefinitionsChildIds() throws Exception {
		this.setRevisionTo("5fdcffa9ccd17743d5498cab0914081fc33606e9");
		SortedSet<XSDImportId> allImports = this.repository.getAllDefinitionsChildIds(XSDImportId.class);
		Assert.assertEquals(1, allImports.size());
	}

	@Test
	public void namespaceWithSpaceAtEndWorks() throws Exception {
		this.setRevisionTo("5fdcffa9ccd17743d5498cab0914081fc33606e9");
		NodeTypeId id = new NodeTypeId("http://www.example.org ", "id", false);
		Assert.assertFalse(this.repository.exists(id));
		this.repository.flagAsExisting(id);
		Assert.assertTrue(this.repository.exists(id));
		Assert.assertNotNull(this.repository.getElement(id));
	}
}
