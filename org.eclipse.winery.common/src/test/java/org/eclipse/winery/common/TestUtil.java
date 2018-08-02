/*******************************************************************************
 * Copyright (c) 2013 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.common;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestUtil {

    @Test
    public void testNamespaceToJavaPackageFullURL() {
        assertEquals("org.example.www.tosca.nodetypes", Util.namespaceToJavaPackage("http://www.example.org/tosca/nodetypes"));
    }

    @Test
    public void testNamespaceToJavaPackageURLWithHostOnly() {
        assertEquals("org.example.www", Util.namespaceToJavaPackage("http://www.example.org/"));
    }

    @Test
    public void testNamespaceToJavaPackageURLWithHostOnlyAndNoFinalSlash() {
        assertEquals("org.example.www", Util.namespaceToJavaPackage("http://www.example.org"));
    }

    @Test
    public void testNamespaceToJavaPackageURLWithNoHost() {
        assertEquals("plainNCname", Util.namespaceToJavaPackage("plainNCname"));
    }

    @Test
    public void testNCNameFromURL() {
        assertEquals("http___www.example.org", Util.makeNCName("http://www.example.org"));
    }

    @Test
    public void testNCNameFromNCName() {
        assertEquals("NCName", Util.makeNCName("NCName"));
    }

    @Test
    public void testGetChecksum() throws Exception {
        String text = "my super content of any file which will be hashed using a SHA-256 hash.";
        assertEquals("c0af55785d21197a9fe4c5e9435fa77bb763f386810909e97f646eba7c827df7",
            HashingUtil.getChecksum(text.getBytes(), "SHA-256"));
    }

    @Test
    public void testGetChecksumOfFile() throws Exception {
        File file = new File(ClassLoader.getSystemClassLoader().getResource("org/eclipse/winery/common/invalid.xml").getFile());
        assertEquals("4406bff97249955ef46ea3ae590f9813fd44dcd769b8204cbb702ee6767173b0",
            HashingUtil.getChecksum(file, "SHA-256"));
    }
}
