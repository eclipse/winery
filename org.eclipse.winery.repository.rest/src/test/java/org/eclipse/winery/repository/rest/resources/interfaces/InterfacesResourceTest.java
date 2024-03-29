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

package org.eclipse.winery.repository.rest.resources.interfaces;

import org.eclipse.winery.repository.rest.resources.AbstractResourceTest;

import org.junit.jupiter.api.Test;

public class InterfacesResourceTest extends AbstractResourceTest {

    @Test
    public void createConfigureInterface() throws Exception {
        this.setRevisionTo("origin/plain");
        this.assertNoContentPost("relationshiptypes/http%253A%252F%252Fplain.winery.opentosca.org%252Frelationshiptypes/RelationshipTypeWithoutProperties/interfaces/",
            "entitytypes/interfaces/create_configure_interface.json");
        this.assertGet("relationshiptypes/http%253A%252F%252Fplain.winery.opentosca.org%252Frelationshiptypes/RelationshipTypeWithoutProperties/interfaces/",
            "entitytypes/interfaces/create_configure_interface_result.json");
        this.assertGet("relationshiptypes/http%253A%252F%252Fplain.winery.opentosca.org%252Frelationshiptypes/RelationshipTypeWithoutProperties",
            "entitytypes/interfaces/create_configure_interface_result.xml");
        this.assertGet("relationshiptypes/http%253A%252F%252Fplain.winery.opentosca.org%252Frelationshiptypes/RelationshipTypeWithoutProperties/xml",
            "entitytypes/interfaces/create_configure_interface_result.xml");
    }
    
    @Test
    public void addInputParameters() throws Exception {
        this.setRevisionTo("origin/plain");
        this.assertNoContentPost("nodetypes/http%253A%252F%252Fopentosca.org%252Fnodetypes/MySQL-DBMS/interfaces/",
            "entitytypes/interfaces/add_input_params.json");
        this.assertGet("nodetypes/http%253A%252F%252Fopentosca.org%252Fnodetypes/MySQL-DBMS/interfaces/",
            "entitytypes/interfaces/add_input_params_result.json");
        this.assertGet("nodetypes/http%253A%252F%252Fopentosca.org%252Fnodetypes/MySQL-DBMS",
            "entitytypes/interfaces/add_input_params_result.xml");
        this.assertGet("nodetypes/http%253A%252F%252Fopentosca.org%252Fnodetypes/MySQL-DBMS/xml",
            "entitytypes/interfaces/add_input_params_result.xml");
    }
    
}
