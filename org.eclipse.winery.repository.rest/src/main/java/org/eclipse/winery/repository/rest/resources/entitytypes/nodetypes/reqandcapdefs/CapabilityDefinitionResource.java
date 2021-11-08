/*******************************************************************************
 * Copyright (c) 2012-2013 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.entitytypes.nodetypes.reqandcapdefs;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TCapabilityDefinition;
import org.eclipse.winery.model.tosca.TConstraint;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResource;
import org.eclipse.winery.repository.rest.resources._support.IPersistable;
import org.eclipse.winery.repository.rest.resources._support.collections.IIdDetermination;
import org.eclipse.winery.repository.rest.resources.entitytypes.nodetypes.NodeTypeResource;

/**
 * Implementation similar to RequirementDefinitionResource, but with TCapabilityDefinition instead of
 * TRequirementDefinition
 */
public final class CapabilityDefinitionResource extends AbstractReqOrCapDefResource<TCapabilityDefinition> {

    private final TCapabilityDefinition capDef;

    /**
     * Constructor has to follow the pattern of EnetityTResource as the constructor is invoked by reflection in
     * EntityWithIdCollectionResource
     *
     * @param res the resource this req def is nested in. Has to be of Type "NodeTypeResource". Due to the
     *            implementation of org.eclipse.winery.repository.resources._support.collections.
     *            withId.EntityWithIdCollectionResource.getEntityResourceInstance(EntityT, int), we have to use
     *            "AbstractComponentInstanceResource" as type
     */
    public CapabilityDefinitionResource(IIdDetermination<TCapabilityDefinition> idDetermination, TCapabilityDefinition capDef, int idx, List<TCapabilityDefinition> list, AbstractComponentInstanceResource res) {
        super(idDetermination, capDef, idx, list, (NodeTypeResource) res, CapabilityDefinitionResource.getConstraints(capDef));
        this.capDef = capDef;
    }

    /**
     * Quick hack to avoid internal server error
     */
    public CapabilityDefinitionResource(IIdDetermination<TCapabilityDefinition> idDetermination, TCapabilityDefinition capDef, int idx, List<TCapabilityDefinition> list, IPersistable res) {
        this(idDetermination, capDef, idx, list, (AbstractComponentInstanceResource) res);
    }

    /**
     * Fetch the list of constraints from the given definition. If the list does not exist, the list is created an
     * stored in the given capDef
     */
    public static List<TConstraint> getConstraints(TCapabilityDefinition capDef) {
        List<TConstraint> constraints = capDef.getConstraints();
        if (constraints == null) {
            constraints = new ArrayList<>();
            capDef.setConstraints(constraints);
        }
        return constraints;
    }

    public QName getType() {
        return this.capDef.getCapabilityType();
    }

    @PUT
    @Path("type")
    public Response setType(@FormParam(value = "type") String value) {
        QName qname = QName.valueOf(value);
        this.capDef.setCapabilityType(qname);
        return RestUtils.persist(this.parent);
    }

    @Override
    public String getId(TCapabilityDefinition e) {
        return e.getName();
    }
}
