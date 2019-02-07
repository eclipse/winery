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
package org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.adaptation.problemsolving.SolutionFactory;
import org.eclipse.winery.model.adaptation.problemsolving.SolutionInputData;
import org.eclipse.winery.model.adaptation.problemsolving.SolutionStrategy;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.configuration.Environment;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResourceContainingATopology;
import org.eclipse.winery.repository.rest.resources._support.dataadapter.composeadapter.CompositionData;
import org.eclipse.winery.repository.splitting.Splitting;
import org.eclipse.winery.repository.targetallocation.Allocation;
import org.eclipse.winery.repository.targetallocation.util.AllocationRequest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyTemplateResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyTemplateResource.class);

    private final TTopologyTemplate topologyTemplate;

    private final AbstractComponentInstanceResourceContainingATopology parent;
    private final String type;

    /**
     * A topology template is always nested in a service template
     */
    public TopologyTemplateResource(AbstractComponentInstanceResourceContainingATopology parent, TTopologyTemplate topologyTemplate, String type) {
        this.topologyTemplate = topologyTemplate;
        this.parent = parent;
        this.type = type;
    }

    @GET
    @ApiOperation(value = "?edit is used in the URL to get the jsPlumb-based editor")
    @Produces(MediaType.TEXT_HTML)
    // @formatter:off
    public Response getHTML(
        @QueryParam(value = "edit") String edit,
        @QueryParam(value = "script") @ApiParam(value = "the script to include in a <script> tag. The function wineryViewExternalScriptOnLoad if it is defined. Only available if 'view' is also set") String script,
        @QueryParam(value = "view") String view,
        @QueryParam(value = "autoLayoutOnLoad") String autoLayoutOnLoad,
        @Context UriInfo uriInfo) {
        // @formatter:on
        Response res;
        String JSPName;
        String location = Environment.getUrlConfiguration().getTopologyModelerUrl();
        location = uriInfo.getBaseUri().resolve(location).toString();
        // at the topology modeler, jersey needs to have an absolute path
        URI repositoryURI = uriInfo.getBaseUri();
        location = location + "/?repositoryURL=";
        location = location + Util.URLencode(repositoryURI.toString());
        ServiceTemplateId serviceTemplate = (ServiceTemplateId) this.parent.getId();
        location = location + "&ns=";
        location = location + serviceTemplate.getNamespace().getEncoded();
        location = location + "&id=";
        location = location + serviceTemplate.getXmlId().getEncoded();
        if (edit == null) {
            // TODO: Render-only mode
            // currently also the edit mode
            URI uri = RestUtils.createURI(location);
            res = Response.seeOther(uri).build();
        } else {
            // edit mode
            URI uri = RestUtils.createURI(location);
            res = Response.seeOther(uri).build();
        }
        return res;
    }

    // @formatter:off
    @GET
    @ApiOperation(value = "Returns a JSON representation of the topology template." +
        "X and Y coordinates are embedded as attributes. QName string with Namespace: " +
        "{@link org.eclipse.winery.repository.common.constants.Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE}" +
        "@return The JSON representation of the topology template <em>without</em> associated artifacts and without the parent service template")
    @Produces(MediaType.APPLICATION_JSON)
    // @formatter:on
    public TTopologyTemplate getComponentInstanceJSON() {
        return this.topologyTemplate;
    }

    /**
     * Only works for relationship templates connecting node templates; and not capabilities
     */
    @Path("merge/")
    @Consumes(MediaType.TEXT_PLAIN)
    @POST
    public Response mergeWithOtherTopologyTemplate(String strOtherServiceTemplateQName) {
        QName otherServiceTemplateQName = QName.valueOf(strOtherServiceTemplateQName);
        ServiceTemplateId otherServiceTemplateId = new ServiceTemplateId(otherServiceTemplateQName);
        ServiceTemplateId thisServiceTemplateId = (ServiceTemplateId) this.parent.getId();
        try {
            BackendUtils.mergeTopologyTemplateAinTopologyTemplateB(otherServiceTemplateId, thisServiceTemplateId);
        } catch (IOException e) {
            LOGGER.debug("Could not merge", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }

        return Response.noContent().build();
    }

    @Path("nodetemplates/")
    public NodeTemplatesResource getNodeTemplatesResource() {
        // FIXME: onDelete will not work as we have a copy of the original list. We have to add a "listener" to remove at the list and route that remove to the original list
        List<TNodeTemplate> l = this.topologyTemplate.getNodeTemplates();
        return new NodeTemplatesResource(l, this.parent);
    }

    @Path("relationshiptemplates/")
    public RelationshipTemplatesResource getRelationshipTemplatesResource() {
        // FIXME: onDelete will not work. See getNodeTemplatesResource
        List<TRelationshipTemplate> l = new ArrayList<>();
        for (TEntityTemplate t : this.topologyTemplate.getNodeTemplateOrRelationshipTemplate()) {
            if (t instanceof TRelationshipTemplate) {
                l.add((TRelationshipTemplate) t);
            }
        }
        return new RelationshipTemplatesResource(l, this.parent);
    }

    @PUT
    @ApiOperation(value = "Replaces the topology by the information given in the XML")
    @Consumes(MediaType.TEXT_XML)
    public Response setModel(TTopologyTemplate topologyTemplate) {
        this.parent.setTopology(topologyTemplate, this.type);
        return RestUtils.persist(this.parent);
    }

    @PUT
    @ApiOperation(value = "Replaces the topology by the information given in the JSON")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setModelJson(TTopologyTemplate topologyTemplate) throws Exception {
        ModelUtilities.patchAnyAttributes(topologyTemplate.getNodeTemplates());
        ModelUtilities.patchAnyAttributes(topologyTemplate.getRelationshipTemplates());
        // the following method includes patching of the topology template (removing empty lists, ..)
        this.parent.setTopology(topologyTemplate, this.type);
        return RestUtils.persist(this.parent);
    }

    // @formatter:off
    @GET
    @ApiOperation(value = "<p>Returns an XML representation of the topology template." +
        " X and Y coordinates are embedded as attributes. Namespace:" +
        "{@link org.eclipse.winery.repository.common.constants.Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE} </p>" +
        "<p>{@link org.eclipse.winery.repository.client.WineryRepositoryClient." +
        "getTopologyTemplate(QName)} consumes this template</p>" +
        "<p>@return The XML representation of the topology template <em>without</em>" +
        "associated artifacts and without the parent service template </p>")
    @Produces( {MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    // @formatter:on
    public Response getComponentInstanceXML() {
        return RestUtils.getXML(TTopologyTemplate.class, this.topologyTemplate);
    }

    @Path("split/")
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response split(@Context UriInfo uriInfo) {
        Splitting splitting = new Splitting();
        ServiceTemplateId splitServiceTemplateId;
        try {
            splitServiceTemplateId = splitting.splitTopologyOfServiceTemplate((ServiceTemplateId) this.parent.getId());
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not split. " + e.getMessage()).build();
        }
        URI url = uriInfo.getBaseUri().resolve(RestUtils.getAbsoluteURL(splitServiceTemplateId));
        return Response.created(url).build();
    }

    @Path("match/")
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response match(@Context UriInfo uriInfo) {
        Splitting splitting = new Splitting();
        ServiceTemplateId matchedServiceTemplateId;
        try {
            matchedServiceTemplateId = splitting.matchTopologyOfServiceTemplate((ServiceTemplateId) this.parent.getId());
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not match. " + e.getMessage()).build();
        }
        URI url = uriInfo.getBaseUri().resolve(RestUtils.getAbsoluteURL(matchedServiceTemplateId));
        return Response.created(url).build();
    }

    @Path("allocate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response allocate(@Context UriInfo uriInfo, AllocationRequest allocationRequest) {
        try {
            Allocation allocation = new Allocation(allocationRequest);
            List<ServiceTemplateId> allocatedIds = allocation.allocate((ServiceTemplateId) this.parent.getId());
            List<URI> urls = new ArrayList<>();
            for (ServiceTemplateId id : allocatedIds) {
                urls.add(uriInfo.getBaseUri().resolve(RestUtils.getAbsoluteURL(id)));
            }
            return Response.ok(urls, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.debug("Error allocating", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("compose/")
    @Consumes( {MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON})
    @Produces( {MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON})
    public Response composeServiceTemplates(CompositionData compositionData, @Context UriInfo uriInfo) {
        Splitting splitting = new Splitting();
        String newComposedSolutionServiceTemplateId = compositionData.getTargetid();
        List<ServiceTemplateId> compositionServiceTemplateIDs = new ArrayList<>();
        compositionData.getCspath().stream().forEach(entry -> {
            QName qName = QName.valueOf(entry);
            compositionServiceTemplateIDs.add(new ServiceTemplateId(qName.getNamespaceURI(), qName.getLocalPart(), false));
        });

        ServiceTemplateId composedServiceTemplateId;

        try {
            composedServiceTemplateId = splitting.composeServiceTemplates(newComposedSolutionServiceTemplateId, compositionServiceTemplateIDs);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        Response mergeResponse = this.mergeWithOtherTopologyTemplate(composedServiceTemplateId.getQName().toString());
        if (mergeResponse.getStatus() == 500) {
            return mergeResponse;
        }
        URI url = uriInfo.getBaseUri().resolve(RestUtils.getAbsoluteURL(parent.getId()));
        String location = url.toString();
        location = location + "topologytemplate?edit";
        url = RestUtils.createURI(location);
        LOGGER.debug("URI of the composed Service Template {}", url.toString());
        return Response.created(url).build();
    }

    @POST
    @Path("resolve/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response resolve(@Context UriInfo uriInfo) {
        Splitting splitting = new Splitting();
        try {
            splitting.resolveTopologyTemplate((ServiceTemplateId) this.parent.getId());
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("applysolution")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TTopologyTemplate applySolution(SolutionInputData data) {
        SolutionStrategy strategy = SolutionFactory.getSolution(data);
        if (strategy.applySolution(this.topologyTemplate, data)) {
            RestUtils.persist(parent);
        } else {
            throw new InternalError("Could not apply the given algorithm to the topology!");
        }

        return this.topologyTemplate;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("determinestatefulcomponents")
    public TTopologyTemplate determineStatefulComponents() {

        return this.topologyTemplate;
    }
}
