/********************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.compliancerules;

import java.io.IOException;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTComplianceRule;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResourceContainingATopology;
import org.eclipse.winery.repository.rest.resources._support.IHasName;
import org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates.TopologyTemplateResource;

public class ComplianceRuleResource extends AbstractComponentInstanceResourceContainingATopology implements IHasName {

    private static final String IDENTIFIER = "identifier";
    private static final String REQUIRED_STRUCTURE = "required-structure";

    /**
     * Instantiates the resource. Assumes that the resource should exist (assured by the caller)
     *
     * The caller should <em>not</em> create the resource by other ways. E.g., by instantiating this resource and then
     * adding data.
     */
    public ComplianceRuleResource(DefinitionsChildId id) {
        super(id);
    }

    @Override
    protected TExtensibleElements createNewElement() {
        return new OTComplianceRule(new OTComplianceRule.Builder("default"));
    }

    public OTComplianceRule getComplianceRule() {
        return (OTComplianceRule) this.getElement();
    }

    @Override
    public String getName() {
        String name = this.getComplianceRule().getName();
        if (name == null) {
            // place default
            name = this.getId().getXmlId().getDecoded();
        }
        return name;
    }

    @Override
    public Response setName(String name) {
        this.getComplianceRule().setName(name);
        return RestUtils.persist(this);
    }

    @Path("identifier/")
    public TopologyTemplateResource getIdentifier() {
        return new TopologyTemplateResource(this, this.getComplianceRule().getIdentifier(), IDENTIFIER);
    }

    @Path("requiredstructure/")
    public TopologyTemplateResource getRequiredStructure() {
        return new TopologyTemplateResource(this, this.getComplianceRule().getRequiredStructure(), REQUIRED_STRUCTURE);
    }

    @Override
    public void setTopology(TTopologyTemplate topologyTemplate, String type) {
        switch (type) {
            case IDENTIFIER:
                this.getComplianceRule().setIdentifier(topologyTemplate);
                break;
            case REQUIRED_STRUCTURE:
                this.getComplianceRule().setRequiredStructure(topologyTemplate);
                break;
            default:
                break;
        }
    }

    @Override
    public TTopologyTemplate getTopology() {
        // TODO this is only here to have SOME implementation
        return this.getComplianceRule().getIdentifier();
    }

    @Override
    protected void synchronizeReferences() throws IOException {
        // no synchronizing needed
    }
}
