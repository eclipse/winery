/*******************************************************************************
 * Copyright (c) 2012-2018 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources._support;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.ApiOperation;

/**
 * Ensures that the AbstractComponentInstance has a getName method
 */
public interface IHasName {

    @GET
    @Path("name")
    // @formatter:off
    @ApiOperation(value = "Returns the name of the element. " +
        "Defaults to the ID of the element. " +
        "Some other ComponentInstances either carry a name or an ID. ")
    // @formatter:on
    @Produces(MediaType.TEXT_PLAIN)
    String getName();

    @PUT
    @Path("name")
    Response setName(@FormParam("value") String name);
}
