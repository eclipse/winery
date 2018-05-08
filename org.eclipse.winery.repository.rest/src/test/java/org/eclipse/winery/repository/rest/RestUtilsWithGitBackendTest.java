/********************************************************************************
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
 ********************************************************************************/
package org.eclipse.winery.repository.rest;

import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.version.WineryVersion;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class RestUtilsWithGitBackendTest extends TestWithGitBackedRepository {

    @Test
    public void renameDefinitionWithoutVersion() throws Exception {
        this.setRevisionTo("origin/plain");
        String oldName = "NodeTypeWithOneReqCapPairWithoutProperties";
        String newName = "exampleName";
        String namespace = "http://plain.winery.opentosca.org/nodetypes";
        DefinitionsChildId oldId = new NodeTypeId(namespace, oldName, false);
        DefinitionsChildId newId = new NodeTypeId(namespace, newName, false);

        Response response = RestUtils.rename(oldId, newId).getResponse();

        String expectedEntity = "http://localhost:8080/winery/nodetypes/"
            + Util.URLencode(Util.URLencode(namespace)) + "/" + newName + "/";

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(expectedEntity, response.getEntity());
    }

    @Test
    public void renameDefinitionNamespaceWithoutVersion() throws Exception {
        this.setRevisionTo("origin/plain");
        String name = "NodeTypeWithOneReqCapPairWithoutProperties";
        String newNamespace = "http://example.org/nodetypes";
        String oldNamespace = "http://plain.winery.opentosca.org/nodetypes";
        DefinitionsChildId oldId = new NodeTypeId(oldNamespace, name, false);
        DefinitionsChildId newId = new NodeTypeId(newNamespace, name, false);

        Response response = RestUtils.rename(oldId, newId).getResponse();

        String expectedEntity = "http://localhost:8080/winery/nodetypes/"
            + Util.URLencode(Util.URLencode(newNamespace)) + "/" + name + "/";

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(expectedEntity, response.getEntity());
    }

    @Test
    public void renameSingleDefinitionWithVersion() throws Exception {
        this.setRevisionTo("origin/plain");
        String version = "_0.3.4-w3";
        String oldName = "NodeTypeWith5Versions";
        String newName = "exampleName";
        String namespace = "http://opentosca.org/nodetypes";
        DefinitionsChildId oldId = new NodeTypeId(namespace, oldName + version, false);
        DefinitionsChildId newId = new NodeTypeId(namespace, newName + version, false);

        Response response = RestUtils.rename(oldId, newId).getResponse();

        String expectedEntity = "http://localhost:8080/winery/nodetypes/"
            + Util.URLencode(Util.URLencode(namespace)) + "/" + newName + version + "/";

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(expectedEntity, response.getEntity());
        Assert.assertEquals(5, BackendUtils.getAllVersionsOfOneDefinition(oldId).size());
        Assert.assertEquals(1, BackendUtils.getAllVersionsOfOneDefinition(newId).size());
    }

    @Test
    public void renameSingleDefinitionNamespaceWithVersion() throws Exception {
        this.setRevisionTo("origin/plain");
        String version = "_0.3.4-w3";
        String name = "NodeTypeWith5Versions";
        String oldNamespace = "http://opentosca.org/nodetypes";
        String newNamespace = "http://example.org/nodetypes";
        DefinitionsChildId oldId = new NodeTypeId(oldNamespace, name + version, false);
        DefinitionsChildId newId = new NodeTypeId(newNamespace, name + version, false);

        Response response = RestUtils.rename(oldId, newId).getResponse();

        String expectedEntity = "http://localhost:8080/winery/nodetypes/"
            + Util.URLencode(Util.URLencode(newNamespace)) + "/" + name + version + "/";

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(expectedEntity, response.getEntity());
        Assert.assertEquals(5, BackendUtils.getAllVersionsOfOneDefinition(oldId).size());
        Assert.assertEquals(1, BackendUtils.getAllVersionsOfOneDefinition(newId).size());
    }

    @Test
    public void renameSingleDefinitionWhichHasChanges() throws Exception {
        this.setRevisionTo("origin/plain");
        String version = "_0.3.4-w3";
        String otherVersion = version + "-wip3";
        String oldName = "NodeTypeWith5Versions";
        String newName = "exampleName";
        String namespace = "http://opentosca.org/nodetypes";
        NodeTypeId oldId = new NodeTypeId(namespace, oldName + version, false);
        DefinitionsChildId newId = new NodeTypeId(namespace, newName + version, false);
        // required because oldId doesn't exist anymore after the rename
        DefinitionsChildId otherElement = new NodeTypeId(namespace, oldName + otherVersion, false);

        makeSomeChanges(oldId);
        Response response = RestUtils.rename(oldId, newId).getResponse();

        String expectedEntity = "http://localhost:8080/winery/nodetypes/"
            + Util.URLencode(Util.URLencode(namespace)) + "/" + newName + version + "/";

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(expectedEntity, response.getEntity());
        Assert.assertEquals(4, BackendUtils.getAllVersionsOfOneDefinition(otherElement).size());
        Assert.assertEquals(1, BackendUtils.getAllVersionsOfOneDefinition(newId).size());
    }

    @Test
    public void renameAllVersionNamesOfOneComponent() throws Exception {
        this.setRevisionTo("origin/plain");
        String version = "_0.3.4-w3";
        String oldName = "NodeTypeWith5Versions";
        String newName = "exampleName";
        String namespace = "http://opentosca.org/nodetypes";
        DefinitionsChildId oldId = new NodeTypeId(namespace, oldName + version, false);
        DefinitionsChildId newId = new NodeTypeId(namespace, newName + version, false);

        Response response = RestUtils.renameAllVersionsOfOneDefinition(oldId, newId);

        String expectedEntity = "http://localhost:8080/winery/nodetypes/"
            + Util.URLencode(Util.URLencode(namespace)) + "/" + newName + version + "/";

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(expectedEntity, response.getEntity());
        Assert.assertEquals(5, BackendUtils.getAllVersionsOfOneDefinition(oldId).size());
        Assert.assertEquals(5, BackendUtils.getAllVersionsOfOneDefinition(newId).size());
    }

    @Test
    public void renameAllVersionNamespacesOfOneComponent() throws Exception {
        this.setRevisionTo("origin/plain");
        String version = "_0.3.4-w3";
        String name = "NodeTypeWith5Versions";
        String oldNamespace = "http://opentosca.org/nodetypes";
        String newNamespace = "http://example.org/nodetypes";
        DefinitionsChildId oldId = new NodeTypeId(oldNamespace, name + version, false);
        DefinitionsChildId newId = new NodeTypeId(newNamespace, name + version, false);

        Response response = RestUtils.renameAllVersionsOfOneDefinition(oldId, newId);

        String expectedEntity = "http://localhost:8080/winery/nodetypes/"
            + Util.URLencode(Util.URLencode(newNamespace)) + "/" + name + version + "/";

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(expectedEntity, response.getEntity());
        Assert.assertEquals(5, BackendUtils.getAllVersionsOfOneDefinition(oldId).size());
        Assert.assertEquals(5, BackendUtils.getAllVersionsOfOneDefinition(newId).size());
    }

    @Test
    public void getFlagsOfAReleasedVersion() throws Exception {
        this.setRevisionTo("origin/plain");
        DefinitionsChildId id = new NodeTypeId("http://opentosca.org/nodetypes", "NodeTypeWith5Versions_0.3.4-w3", false);

        WineryVersion version = BackendUtils.getCurrentVersionWithAllFlags(id);

        Assert.assertFalse(version.isReleasable());
        Assert.assertFalse(version.isEditable());
        Assert.assertTrue(version.isCurrentVersion());
        Assert.assertTrue(version.isLatestVersion());
    }

    @Test
    public void getFlagsOfAReleasableVersion() throws Exception {
        this.setRevisionTo("d920a1a37e3e1c3be32bf282a4d240d83811fdb1");
        DefinitionsChildId id = new NodeTypeId("http://plain.winery.opentosca.org/nodetypes", "NodeTypeWithImplementation_1.0-w1-wip1", false);

        WineryVersion version = BackendUtils.getCurrentVersionWithAllFlags(id);

        Assert.assertTrue(version.isReleasable());
        Assert.assertTrue(version.isCurrentVersion());
        Assert.assertTrue(version.isLatestVersion());
        Assert.assertFalse(version.isEditable());
    }

    @Test
    public void releaseComponentWhichDoesNotHaveChanges() throws Exception {
        this.setRevisionTo("origin/plain");
        DefinitionsChildId id = new NodeTypeId("http://opentosca.org/nodetypes", "NodeTypeWithALowerReleasableManagementVersion_2-w2-wip1", false);
        DefinitionsChildId releasedId = new NodeTypeId("http://opentosca.org/nodetypes", "NodeTypeWithALowerReleasableManagementVersion_2-w2", false);

        int formerVersionCount = BackendUtils.getAllVersionsOfOneDefinition(id).size();

        Response response = RestUtils.releaseVersion(id);

        int finalVersionCount = BackendUtils.getAllVersionsOfOneDefinition(releasedId).size();
        WineryVersion version = BackendUtils.getCurrentVersionWithAllFlags(releasedId);

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(formerVersionCount + 1, finalVersionCount);
        Assert.assertFalse(version.isReleasable());
        Assert.assertTrue(version.getWorkInProgressVersion() == 0);
    }

    @Test
    public void releaseComponentWhichHasNotBeenCommitted() throws Exception {
        this.setRevisionTo("origin/plain");
        NodeTypeId id = new NodeTypeId("http://opentosca.org/nodetypes", "NodeTypeWithALowerReleasableManagementVersion_2-w2-wip1", false);
        NodeTypeId releasedId = new NodeTypeId("http://opentosca.org/nodetypes", "NodeTypeWithALowerReleasableManagementVersion_2-w2", false);

        int formerVersionCount = BackendUtils.getAllVersionsOfOneDefinition(id).size();

        // simulate a non-committed component
        makeSomeChanges(id);

        Response response = RestUtils.releaseVersion(id);

        int finalVersionCount = BackendUtils.getAllVersionsOfOneDefinition(releasedId).size();
        WineryVersion version = BackendUtils.getCurrentVersionWithAllFlags(releasedId);

        Assert.assertEquals(201, response.getStatus());
        Assert.assertEquals(formerVersionCount + 1, finalVersionCount);
        Assert.assertFalse(version.isReleasable());
        Assert.assertTrue(version.getWorkInProgressVersion() == 0);
    }
}
