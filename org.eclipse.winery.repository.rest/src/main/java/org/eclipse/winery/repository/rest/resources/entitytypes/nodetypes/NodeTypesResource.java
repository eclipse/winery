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
package org.eclipse.winery.repository.rest.resources.entitytypes.nodetypes;

import io.swagger.annotations.Api;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentsResource;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentsWithoutTypeReferenceResource;
import org.eclipse.winery.repository.rest.resources.apiData.NodeTypesVisualsApiData;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Manages all nodetypes in all available namespaces.
 * The actual implementation is done in the AbstractComponentsResource
 */
@Api(tags = "Node Types")
public class NodeTypesResource extends AbstractComponentsWithoutTypeReferenceResource<NodeTypeResource> {

    @Path("{namespace}/{id}/")
    public NodeTypeResource getComponentInstanceResource(@PathParam("namespace") String namespace, @PathParam("id") String id) {
        return this.getComponentInstanceResource(namespace, id, true);
    }

    @GET
    @Path("allvisualappearancedata")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NodeTypesVisualsApiData> getVisualAppearanceList() {
        SortedSet<NodeTypeId> allNodeTypeIds = RepositoryFactory.getRepository().getAllDefinitionsChildIds(NodeTypeId.class);
        return allNodeTypeIds.stream()
            .map(id -> {
                NodeTypeResource res = (NodeTypeResource) AbstractComponentsResource.getComponentInstanceResource(id);
                return res.getVisualAppearanceResource().getJsonData();
            })
            .collect(Collectors.toList());
    }
}
