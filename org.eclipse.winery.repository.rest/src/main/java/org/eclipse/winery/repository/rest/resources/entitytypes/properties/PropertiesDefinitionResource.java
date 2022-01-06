/*******************************************************************************
 * Copyright (c) 2012-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.entitytypes.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.extensions.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.xsd.NamespaceAndDefinedLocalNames;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.datatypes.NamespaceAndDefinedLocalNamesForAngular;
import org.eclipse.winery.repository.rest.resources.apiData.InheritedPropertiesDefinitionsResourceApiData;
import org.eclipse.winery.repository.rest.resources.apiData.PropertiesDefinitionEnum;
import org.eclipse.winery.repository.rest.resources.apiData.PropertiesDefinitionResourceApiData;
import org.eclipse.winery.repository.rest.resources.entitytypes.EntityTypeResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models
 * <ol>
 * <li>TOSCA conforming properties definition (XML element / XML schema / none)</li>
 * <li>Winery's KV properties (in the subresource "winery")</li>
 * <li>YAML property definitions!</li>
 * </ol>
 * <p>
 */
public class PropertiesDefinitionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesDefinitionResource.class);

    // We hold a copy of super.res as we work on the type EntityTypeResource instead of AbstractComponentInstanceResource
    private final EntityTypeResource parentRes;

    // we assume that this class is created at each request
    // therefore, we can have "wpd" final
    private final WinerysPropertiesDefinition wpd;


    public PropertiesDefinitionResource(EntityTypeResource res) {
        this.parentRes = res;
        this.wpd = res.getEntityType().getWinerysPropertiesDefinition();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PropertiesDefinitionResourceApiData getJson() {
        return new PropertiesDefinitionResourceApiData(this.getEntityType().getProperties(), this.wpd);
    }

    // TODO: implement getMerged endpoint
    // TODO: do we require deep copies?
    // TODO: this merges only wineryPropertiesDefinitions?
    // TODO: do we need to merge entityType#getProperties or something else?
    // TODO: does not know at the end from which entityType the property definition has been derived from
    @GET
    @Path("merged")
    @Produces(MediaType.APPLICATION_JSON)
    public PropertiesDefinitionResourceApiData getMerged() {
        // Abort if winerysPropertyDefinitions is not present
        if (this.wpd == null) {
            return new PropertiesDefinitionResourceApiData(this.getEntityType().getProperties(), this.wpd);
        }
        
        ArrayList<TEntityType> parents = RepositoryFactory.getRepository().getParents(this.parentRes.getEntityType());
        
        // NOTE: this is not a deep copy but a reference!
        List<PropertyDefinitionKV> propertyDefinitions = this.getEntityType().getWinerysPropertiesDefinition().getPropertyDefinitions();

        for (TEntityType parent : parents) {
            WinerysPropertiesDefinition winerysPropertiesDefinition = parent.getWinerysPropertiesDefinition();
            // Abort if winerysPropertyDefinitions is not present
            if (winerysPropertiesDefinition == null) {
                break;
            }
            
            for (PropertyDefinitionKV parentPropertyDefinition : winerysPropertiesDefinition.getPropertyDefinitions()) {
                // Find property definition of parent in child
                boolean exists = false;
                for (PropertyDefinitionKV propertyDefinition : propertyDefinitions) {
                    if (Objects.equals(propertyDefinition.getKey(), parentPropertyDefinition.getKey())) {
                        exists = true;
                        break;
                    }
                }
                
                // Add property definition of parent if not found
                if (!exists) {
                    propertyDefinitions.add(parentPropertyDefinition);
                }
            }
        }
        
        return new PropertiesDefinitionResourceApiData(this.getEntityType().getProperties(), this.getEntityType().getWinerysPropertiesDefinition());
    }

    @GET
    @Path("inherited")
    @Produces(MediaType.APPLICATION_JSON)
    public List<InheritedPropertiesDefinitionsResourceApiData> getInheritedPropertiesDefinitionResource() {
        ArrayList<TEntityType> parents = RepositoryFactory.getRepository().getParents(this.parentRes.getEntityType());
        
        ArrayList<InheritedPropertiesDefinitionsResourceApiData> list = new ArrayList<>();
        for (TEntityType parent : parents) {
            WinerysPropertiesDefinition winerysPropertiesDefinition = parent.getWinerysPropertiesDefinition();
            if (winerysPropertiesDefinition != null) {
                PropertiesDefinitionResourceApiData propertiesDefinitionResourceApiData = new PropertiesDefinitionResourceApiData(parent.getProperties(), winerysPropertiesDefinition);
                list.add(new InheritedPropertiesDefinitionsResourceApiData(parent.getQName(), propertiesDefinitionResourceApiData));
            }
        }

        return list;
    }

    @GET
    @Path("{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NamespaceAndDefinedLocalNamesForAngular> getXsdDefinitionJson(@PathParam("type") String type) {
        List<NamespaceAndDefinedLocalNames> allDeclaredElementsLocalNames = null;
        switch (type) {
            case "element":
                allDeclaredElementsLocalNames = RepositoryFactory.getRepository().getXsdImportManager().getAllDeclaredElementsLocalNames();
                break;
            case "type":
                allDeclaredElementsLocalNames = RepositoryFactory.getRepository().getXsdImportManager().getAllDefinedTypesLocalNames();
                break;
        }

        if (allDeclaredElementsLocalNames == null) {
            LOGGER.error("No such parameter \"{}\" available in this call", type);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        return RestUtils.convert(allDeclaredElementsLocalNames);
    }

    public TEntityType getEntityType() {
        return this.parentRes.getEntityType();
    }

    @DELETE
    public Response clearPropertiesDefinition() {
        this.getEntityType().setProperties(null);
        ModelUtilities.removeWinerysPropertiesDefinition(this.getEntityType());
        return RestUtils.persist(this.parentRes);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response onJsonPost(PropertiesDefinitionResourceApiData data) {
        if (data.selectedValue == PropertiesDefinitionEnum.Element || data.selectedValue == PropertiesDefinitionEnum.Type) {
            // first of all, remove Winery's Properties definition (if it exists)
            ModelUtilities.removeWinerysPropertiesDefinition(this.getEntityType());
            // replace old properties definition by new one
            // FIXME need to actually handle propertiesData properly!
            if (data.propertiesDefinition == null) {
                return Response.status(Status.BAD_REQUEST).entity("Wrong data submitted!").build();
            }

            this.getEntityType().setProperties(data.propertiesDefinition);
            List<String> errors = new ArrayList<>();
            BackendUtils.deriveWPD(this.getEntityType(), errors, RepositoryFactory.getRepository());
            // currently the errors are just logged
            for (String error : errors) {
                PropertiesDefinitionResource.LOGGER.debug(error);
            }
            return RestUtils.persist(this.parentRes);
        } else if (data.selectedValue == PropertiesDefinitionEnum.Custom) {
            TEntityType et = this.parentRes.getEntityType();
            et.setProperties(data.winerysPropertiesDefinition);
            return RestUtils.persist(this.parentRes);
        } else if (data.selectedValue == PropertiesDefinitionEnum.Yaml) {
            TEntityType et = this.parentRes.getEntityType();
            if (!(data.propertiesDefinition instanceof TEntityType.YamlPropertiesDefinition)) {
                return Response.status(Status.BAD_REQUEST).entity("Expected YamlPropertiesDefinition element").build();
            }
            et.setProperties(data.propertiesDefinition);
            return RestUtils.persist(this.parentRes);
        }

        return Response.status(Status.BAD_REQUEST).entity("Wrong data submitted!").build();
    }
}
