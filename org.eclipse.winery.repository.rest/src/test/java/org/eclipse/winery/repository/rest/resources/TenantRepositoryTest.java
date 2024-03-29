/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.rest.resources;

import org.eclipse.winery.common.configuration.Environments;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@Disabled("For some reason, SOMETIMES, not always!, JGit does not find 313f40a0a42111d3d36aef0fa36a9ec82a997e14 after running this test." +
    "However, it worked before...")
public class TenantRepositoryTest extends AbstractResourceTest {

    static boolean workspaceRepositoryTenant;

    @BeforeAll
    public static void init() throws Exception {
        workspaceRepositoryTenant = Environments.getInstance().getRepositoryConfig().isTenantRepository();
        Environments.getInstance().getRepositoryConfig().setTenantRepository(true);
        AbstractResourceTest.init();
    }

    @AfterAll
    public static void shutdown() throws Exception {
        AbstractResourceTest.shutdown();
        Environments.getInstance().getRepositoryConfig().setTenantRepository(workspaceRepositoryTenant);
    }

    @Test
    public void testTenantRepository() throws Exception {
        this.setRevisionTo("origin/tenants");

        String[] results = new String[3];

        Thread tenant1Thread = new Thread(() ->
            results[0] = start()
                .accept(ContentType.JSON.toString())
                .get(callURL("servicetemplates/?xTenant=tenant_1"))
                .then()
                .log()
                .all()
                .statusCode(200)
                .extract()
                .response()
                .getBody()
                .asString()
        );

        Thread tenant2Thread = new Thread(() ->
            results[1] = start()
                .accept(ContentType.JSON.toString())
                .header(new Header("xTenant", "tenant_2"))
                .get(callURL("servicetemplates/"))
                .then()
                .log()
                .all()
                .statusCode(200)
                .extract()
                .response()
                .getBody()
                .asString()
        );

        Thread tenant3Thread = new Thread(() ->
            results[2] = start()
                .accept(ContentType.JSON.toString())
                .header(new Header("xTenant", "tenant_3"))
                .get(callURL("servicetemplates/"))
                .then()
                .log()
                .all()
                .statusCode(200)
                .extract()
                .response()
                .getBody()
                .asString()
        );

        tenant1Thread.start();
        tenant2Thread.start();
        tenant3Thread.start();

        tenant1Thread.join();
        tenant2Thread.join();
        tenant3Thread.join();

        JSONAssert.assertEquals(
            readFromClasspath("tenants/tenants1.json"),
            results[0],
            JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(
            readFromClasspath("tenants/tenants2.json"),
            results[1],
            JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(
            readFromClasspath("tenants/tenants3.json"),
            results[2],
            JSONCompareMode.NON_EXTENSIBLE);
    }
}
