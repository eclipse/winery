/*******************************************************************************
 * Copyright (c) 2019-2021 Contributors to the Eclipse Foundation
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTRelationMapping;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources.apiData.VisualsApiData;
import org.eclipse.winery.repository.rest.resources.refinementmodels.RelationMappingsResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates.RefinementTopologyTemplateResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates.TopologyTemplateResource;

public abstract class AbstractRefinementModelResource extends AbstractComponentInstanceResourceContainingATopology implements IHasName {

    protected static final String REFINEMENT_TOPOLOGY = "refinement-structure";
    protected static final String DETECTOR = "detector";
    protected static final String GRAFIC_PRM_MODEL = "grafic-prm-model";
    public List<String> mappingTypes;

    protected AbstractRefinementModelResource(DefinitionsChildId id) {
        super(id);
    }

    public abstract OTRefinementModel getTRefinementModel();

    public abstract TopologyTemplateResource savePrmMappingTopology(TTopologyTemplate topologyTemplate);

    @Path("detector")
    public TopologyTemplateResource getDetectorResource() {
        return new TopologyTemplateResource(this, this.getTRefinementModel().getDetector(), DETECTOR);
    }

    public abstract TopologyTemplateResource getRefinementTopologyResource();

    @Path("relationmappings")
    public RelationMappingsResource getRelationMappings() {
        List<OTRelationMapping> relationMappings = this.getTRefinementModel().getRelationMappings();

        if (Objects.isNull(relationMappings)) {
            relationMappings = new ArrayList<>();
            this.getTRefinementModel().setRelationMappings(relationMappings);
        }

        return new RelationMappingsResource(this, relationMappings);
    }

    @Override
    public String getName() {
        String name = this.getTRefinementModel().getName();
        if (name == null) {
            // place default
            name = this.getId().getXmlId().getDecoded();
        }
        return name;
    }

    @Override
    public Response setName(String name) {
        this.getTRefinementModel().setName(name);
        return RestUtils.persist(this);
    }

    @Override
    protected void synchronizeReferences() {
        // no synchronization needed
    }

    @Override
    public void setTopology(TTopologyTemplate topologyTemplate, String type) {
        switch (type) {
            case DETECTOR:
                this.getTRefinementModel().setDetector(topologyTemplate);
                break;
            case REFINEMENT_TOPOLOGY:
                this.getTRefinementModel().setRefinementTopology(topologyTemplate);
                break;
            default:
                break;
        }
    }

    @Override
    public TTopologyTemplate getTopology() {
        // TODO this is only here to have SOME implementation
        return this.getTRefinementModel().getRefinementTopology();
    }

    @Path("graficprmmodelling")
    public TopologyTemplateResource getGraphicModelingData() {
        return new RefinementTopologyTemplateResource(this, getTRefinementModel(), GRAFIC_PRM_MODEL);
    }

    @GET
    @Path("mappingTypes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TRelationshipType> getMappingTypes() {
        List<TRelationshipType> relationshipTypesForPrmMappingTypes = new ArrayList<>();
        for (String mappingType : mappingTypes) {
            TRelationshipType relType = new TRelationshipType(new
                TRelationshipType.Builder(mappingType));
            relType.setTargetNamespace("http://opentosca.org/prmMappingTypes");
            relationshipTypesForPrmMappingTypes.add(relType);
        }
        return relationshipTypesForPrmMappingTypes;
    }

    @GET
    @Path("mappingVisuals")
    @Produces(MediaType.APPLICATION_JSON)
    public List<VisualsApiData> getMappingRelationshipVisuals() {
        List<VisualsApiData> prmMappingRelationshipVisuals = new ArrayList<>();
        for (String mappingType : mappingTypes) {
            String color;
            switch (mappingType) {
                case "PermutationMapping":
                    color = "#ff5533";
                    break;
                case "RelationshipMapping":
                    color = "#96ff33";
                    break;
                case "DeploymentArtifactMapping":
                    color = "#33ebff";
                    break;
                case "AttributeMapping":
                    color = "#aa33ff";
                    break;
                case "StayMapping":
                    color = "#ffd333";
                    break;
                case "BehaviorPatternMapping":
                    color = "#3347ff";
                    break;
                default:
                    color = "#000000";
                    break;
            }
            VisualsApiData newVisuals = new VisualsApiData(color, QName.valueOf("{http://opentosca.org/prmMappingTypes}" + mappingType));
            prmMappingRelationshipVisuals.add(newVisuals);
        }
        return prmMappingRelationshipVisuals;
    }
}
