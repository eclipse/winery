/*******************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.driverspecificationandinjection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.ids.definitions.ArtifactTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.repository.TestWithGitBackedRepository;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DASpecificationTest extends TestWithGitBackedRepository {

    @Test
    public void getArtifactTypeOfDA() throws Exception {
        setRevisionTo("origin/plain");
        ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/daspecifier", "DASpecificationTest", false);
        TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
        assertNotNull(topologyTemplate);

        TNodeTemplate nodeTemplateWithAbstractDA = topologyTemplate.getNodeTemplate("shetland_pony");
        assertNotNull(nodeTemplateWithAbstractDA);
        assertNotNull(nodeTemplateWithAbstractDA.getDeploymentArtifacts());

        TDeploymentArtifact deploymentArtifact = nodeTemplateWithAbstractDA.getDeploymentArtifacts().get(0);
        QName artifactTypeQName = deploymentArtifact.getArtifactType();
        ArtifactTypeId artifactTypeId = new ArtifactTypeId(artifactTypeQName);

        TArtifactType artifactType = this.repository.getElement(artifactTypeId);

        assertEquals(artifactType.getTargetNamespace(), DASpecification.getArtifactTypeOfDA(nodeTemplateWithAbstractDA.getDeploymentArtifacts().get(0)).getTargetNamespace());
        assertEquals(artifactType.getName(), DASpecification.getArtifactTypeOfDA(nodeTemplateWithAbstractDA.getDeploymentArtifacts().get(0)).getName());
    }

    @Test
    public void getNodeTemplatesWithAbstractDAs() throws Exception {
        setRevisionTo("origin/plain");
        ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/daspecifier", "DASpecificationTest", false);
        TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
        assertNotNull(topologyTemplate);

        List<TNodeTemplate> nodeTemplateWithAbstractDA = new ArrayList<>();
        nodeTemplateWithAbstractDA.add(topologyTemplate.getNodeTemplate("shetland_pony"));

        List<TNodeTemplate> nodesWithAbstractDA = DASpecification.getNodeTemplatesWithAbstractDAs(topologyTemplate);

        assertEquals(nodeTemplateWithAbstractDA, nodesWithAbstractDA);
    }

    @Test
    public void getArtifactTypeHierarchy() throws Exception {
        setRevisionTo("origin/plain");
        ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/daspecifier", "DASpecificationTest", false);
        TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
        assertNotNull(topologyTemplate);

        TNodeTemplate nodeTemplate = topologyTemplate.getNodeTemplate("westernequipment");
        assertNotNull(nodeTemplate);
        assertNotNull(nodeTemplate.getDeploymentArtifacts());

        List<TArtifactType> artifactTypes = DASpecification.getArtifactTypeHierarchy(DASpecification.getArtifactTypeOfDA(nodeTemplate.getDeploymentArtifacts().get(0)));
        List<String> artifactTypeNames = new ArrayList<>();
        artifactTypes.forEach(at -> artifactTypeNames.add(at.getName()));

        assertEquals(2, artifactTypes.size());
        assertTrue(artifactTypeNames.contains("WesternEquipment_Pony"));
        assertTrue(artifactTypeNames.contains("PonyEquipment"));
    }

    @Test
    public void getNodesWithSuitableConcreteDAs() throws Exception {
        setRevisionTo("origin/plain");
        ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/daspecifier", "DASpecificationTest", false);
        TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
        assertNotNull(topologyTemplate);

        TNodeTemplate nodeTemplate = topologyTemplate.getNodeTemplate("ponycompetition");
        TNodeTemplate nodeTemplateWithAbstractDA = topologyTemplate.getNodeTemplate("shetland_pony");
        assertNotNull(nodeTemplateWithAbstractDA);
        assertNotNull(nodeTemplateWithAbstractDA.getDeploymentArtifacts());

        TDeploymentArtifact deploymentArtifact = nodeTemplateWithAbstractDA.getDeploymentArtifacts().get(0);
        TNodeTemplate expectedNodeTemplate = topologyTemplate.getNodeTemplate("dressageequipment");

        TNodeTemplate actualNodeWithConcreteDA = DASpecification.getNodesWithSuitableConcreteDAs(nodeTemplate, deploymentArtifact, topologyTemplate);

        assertEquals(expectedNodeTemplate, actualNodeWithConcreteDA);
    }

    @Test
    public void getNodesWithSuitableConcreteDAAndTheDirectlyConnectedNode() throws Exception {
        setRevisionTo("origin/plain");
        ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/daspecifier", "DASpecificationTest", false);
        TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
        assertNotNull(topologyTemplate);

        TNodeTemplate nodeTemplateWithAbstractDA = topologyTemplate.getNodeTemplate("shetland_pony");
        assertNotNull(nodeTemplateWithAbstractDA);
        assertNotNull(nodeTemplateWithAbstractDA.getDeploymentArtifacts());

        TDeploymentArtifact deploymentArtifact = nodeTemplateWithAbstractDA.getDeploymentArtifacts().get(0);
        TNodeTemplate nodeTemplateConcreteDA1 = topologyTemplate.getNodeTemplate("dressageequipment");
        TRelationshipTemplate relationshipTemplate1 = topologyTemplate.getRelationshipTemplate("con_42");
        TNodeTemplate nodeTemplateConcreteDA2 = topologyTemplate.getNodeTemplate("westernequipment");
        TRelationshipTemplate relationshipTemplate2 = topologyTemplate.getRelationshipTemplate("con_54");

        Set<Pair<TRelationshipTemplate, TNodeTemplate>> concreteDAsAndConnectedNodes = new HashSet<>();
        concreteDAsAndConnectedNodes.add(Pair.of(relationshipTemplate1, nodeTemplateConcreteDA1));
        concreteDAsAndConnectedNodes.add(Pair.of(relationshipTemplate2, nodeTemplateConcreteDA2));

        Set<Pair<TRelationshipTemplate, TNodeTemplate>> actualNodeWithConcreteDA =
            DASpecification.getNodesWithSuitableConcreteDAAndTheDirectlyConnectedNode(nodeTemplateWithAbstractDA, deploymentArtifact, topologyTemplate);

        assertEquals(concreteDAsAndConnectedNodes, actualNodeWithConcreteDA);
    }
}
