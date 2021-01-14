/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.adaptation.enhance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.NodeTypeImplementationId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.common.version.WineryVersion;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.constants.OpenToscaBaseTypes;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnhancementUtilsTestWithGitBackedRepository extends TestWithGitBackedRepository {

    @Test
    public void determineStatefulComponentsTest() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate element = RepositoryFactory.getRepository()
            .getElement(new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/examples/servicetemplates}TopologyWithStatefulComponent_w1-wip1")
                )
            );

        TTopologyTemplate topologyTemplate = EnhancementUtils.determineStatefulComponents(element.getTopologyTemplate());

        TNodeTemplate statefulComponent = topologyTemplate.getNodeTemplate("statefulComponent");
        assertNotNull(statefulComponent);
        TPolicy stateful = statefulComponent.getPolicies().getPolicy().get(0);
        assertEquals(OpenToscaBaseTypes.statefulComponentPolicyType, stateful.getPolicyType());

        TNodeTemplate stateless = topologyTemplate.getNodeTemplate("stateless");
        assertNotNull(stateless);
        assertNull(stateless.getPolicies());
    }

    @Test
    public void determineFreezableComponents() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate element = RepositoryFactory.getRepository()
            .getElement(new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/examples/servicetemplates}TopologyWithStatefulComponent_w1-wip2")
                )
            );
        TPolicy expectedPolicy = new TPolicy();
        expectedPolicy.setPolicyType(OpenToscaBaseTypes.freezableComponentPolicyType);
        expectedPolicy.setName("freezable");

        TopologyAndErrorList result = EnhancementUtils.determineFreezableComponents(element.getTopologyTemplate());

        assertEquals(0, result.errorList.size());

        TTopologyTemplate topologyTemplate = result.topologyTemplate;
        assertNull(topologyTemplate.getNodeTemplate("VM_2").getPolicies());

        List<TPolicy> statefulFreezableComponentPolicies = topologyTemplate.getNodeTemplate("statefulFreezableComponent")
            .getPolicies().getPolicy();
        assertEquals(2, statefulFreezableComponentPolicies.size());
        assertTrue(statefulFreezableComponentPolicies.contains(expectedPolicy));

        List<TPolicy> statefulNotFreezableComponentPolicies = topologyTemplate.getNodeTemplate("statefulNotFreezableComponent")
            .getPolicies().getPolicy();
        assertEquals(1, statefulNotFreezableComponentPolicies.size());
        assertFalse(statefulNotFreezableComponentPolicies.contains(expectedPolicy));

        List<TPolicy> statelessFreezableComponentPolicies = topologyTemplate.getNodeTemplate("statelessFreezableComponent")
            .getPolicies().getPolicy();
        assertEquals(1, statelessFreezableComponentPolicies.size());
        assertTrue(statelessFreezableComponentPolicies.contains(expectedPolicy));

        assertNull(topologyTemplate.getNodeTemplate("AbstractNodeTypeWithProperties_1-w1-wip1").getPolicies());
        assertNull(topologyTemplate.getNodeTemplate("Infrastructure-As-A-Service-Implementation_1-w1-wip1").getPolicies());
    }

    @Test
    public void cleanFreezableComponents() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate element = RepositoryFactory.getRepository()
            .getElement(new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/examples/servicetemplates}TopologyWithStatefulComponent_w1-wip3")
                )
            );

        TPolicy freezablePolicy = new TPolicy();
        freezablePolicy.setPolicyType(OpenToscaBaseTypes.freezableComponentPolicyType);
        freezablePolicy.setName("freezable");

        TTopologyTemplate topologyTemplate = EnhancementUtils.cleanFreezableComponents(element.getTopologyTemplate());
        assertTrue(topologyTemplate.getNodeTemplate("statefulFreezableComponent").getPolicies().getPolicy().contains(freezablePolicy));

        assertFalse(topologyTemplate.getNodeTemplate("statefulNotFreezableComponent").getPolicies().getPolicy().contains(freezablePolicy));
        assertFalse(topologyTemplate.getNodeTemplate("AbstractNodeTypeWithProperties_1-w1-wip1").getPolicies().getPolicy().contains(freezablePolicy));
        assertTrue(topologyTemplate.getNodeTemplate("statelessFreezableComponent").getPolicies().getPolicy().contains(freezablePolicy));

        assertFalse(topologyTemplate.getNodeTemplate("statefulFreezableImplicitlyProvisioned").getPolicies().getPolicy().contains(freezablePolicy));
        assertTrue(topologyTemplate.getNodeTemplate("VM_3").getPolicies().getPolicy().contains(freezablePolicy));
    }

    @Test
    void getAvailableFeatures() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate serviceTemplate = RepositoryFactory.getRepository()
            .getElement(
                new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/add/management/to/instances/servicetemplates}STWithBasicManagementOnly_w1-wip1")
                )
            );

        Map<String, Map<QName, String>> availableFeaturesForTopology =
            // As we do not want to filter the features based on the deployment technology, the second parameter is null.
            EnhancementUtils.getAvailableFeaturesForTopology(serviceTemplate.getTopologyTemplate(), null);

        assertEquals(2, availableFeaturesForTopology.size());
        assertEquals(1, availableFeaturesForTopology.get("MySQL-Database_w1").size());
        assertEquals(2, availableFeaturesForTopology.get("Ubuntu_16.04-w1").size());
    }

    @Test
    void getAvailableFeaturesWhereNoUbuntuIsAvailableInTheStack() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate serviceTemplate = RepositoryFactory.getRepository()
            .getElement(
                new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/add/management/to/instances/servicetemplates}STWithBasicManagementOnly_w1-wip3")
                )
            );

        Map<String, Map<QName, String>> availableFeaturesForTopology =
            // As we do not want to filter the features based on the deployment technology, the second parameter is null.
            EnhancementUtils.getAvailableFeaturesForTopology(serviceTemplate.getTopologyTemplate(), null);

        assertEquals(1, availableFeaturesForTopology.size());
        assertEquals(1, availableFeaturesForTopology.get("MySQL-Database_w2").size()
        );
    }

    @Test
    void getAvailableFeaturesWhereASpecificRequirementIsSatisfied() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate serviceTemplate = RepositoryFactory.getRepository()
            .getElement(
                new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/add/management/to/instances/servicetemplates}STWithBasicManagementOnly_w1-wip2")
                )
            );

        Map<String, Map<QName, String>> availableFeaturesForTopology =
            // As we do not want to filter the features based on the deployment technology, the second parameter is null.
            EnhancementUtils.getAvailableFeaturesForTopology(serviceTemplate.getTopologyTemplate(), null);

        assertEquals(2, availableFeaturesForTopology.size());
        assertEquals(2, availableFeaturesForTopology.get("MySQL-Database_w2").size());
        assertEquals(2, availableFeaturesForTopology.get("Ubuntu_16.04-w1").size());
    }

    @Test
    void mergeFeatureNodeTypeUbuntu() throws Exception {
        this.setRevisionTo("origin/plain");

        Map<QName, TExtensibleElements> previousListOfNodeTypes = this.repository.getQNameToElementMapping(NodeTypeId.class);

        String testingFeatureName = "Testing";
        String freezeFeatureName = "Freeze and Defrost";
        String nodeTemplateId = "myId";

        Map<QName, String> ubuntuFeatures = new HashMap<>();
        ubuntuFeatures.put(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}Ubuntu_16.04-testable-w1"), testingFeatureName);
        ubuntuFeatures.put(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}Ubuntu_16.04-freezable-w1"), freezeFeatureName);

        TNodeTemplate nodeTemplate = new TNodeTemplate();
        nodeTemplate.setType(
            QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}Ubuntu_16.04-w1")
        );
        nodeTemplate.setId(nodeTemplateId);

        TNodeType generatedFeatureEnrichedNodeType = EnhancementUtils.createFeatureNodeType(nodeTemplate, ubuntuFeatures);

        Map<QName, TExtensibleElements> listOfNodeTypes = this.repository.getQNameToElementMapping(NodeTypeId.class);
        QName expectedMergedUbuntuQName = QName.valueOf(
            "{http://opentosca.org/add/management/to/instances/nodetypes" + EnhancementUtils.GENERATED_NS_SUFFIX
                + "}Ubuntu_16.04-w1-" + nodeTemplateId + "-" + "Testing-Freeze_and_Defrost"
                + WineryVersion.WINERY_VERSION_SEPARATOR + WineryVersion.WINERY_VERSION_PREFIX + "1"
        );

        assertEquals(1, listOfNodeTypes.size() - previousListOfNodeTypes.size());
        assertEquals(expectedMergedUbuntuQName, generatedFeatureEnrichedNodeType.getQName());
        assertNotNull(generatedFeatureEnrichedNodeType.getWinerysPropertiesDefinition());
        assertEquals(9, generatedFeatureEnrichedNodeType.getWinerysPropertiesDefinition().getPropertyDefinitions().size());

        TNodeTypeImplementation generatedUbuntuImpl = this.repository.getElement(
            new ArrayList<>(this.repository.getAllElementsReferencingGivenType(NodeTypeImplementationId.class, expectedMergedUbuntuQName))
                .get(0)
        );

        assertNotNull(generatedUbuntuImpl);
        assertNotNull(generatedUbuntuImpl.getImplementationArtifacts());
        assertEquals(3, generatedUbuntuImpl.getImplementationArtifacts().getImplementationArtifact().size());
    }

    @Test
    void applyFeaturesToTopology() throws Exception {
        this.setRevisionTo("origin/plain");

        TTopologyTemplate topology = RepositoryFactory.getRepository()
            .getElement(
                new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/add/management/to/instances/servicetemplates}STWithBasicManagementOnly_w1-wip1")
                )
            ).getTopologyTemplate();

        // As we do not want to filter the features based on the deployment technology, the second parameter is null.
        EnhancementUtils.applyFeaturesForTopology(topology, EnhancementUtils.getAvailableFeaturesForTopology(topology, null));

        String ubuntuNodeTemplateId = "Ubuntu_16.04-w1";
        String mySqlNodeTemplateId = "MySQL-Database_w1";

        assertEquals(
            QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes" + EnhancementUtils.GENERATED_NS_SUFFIX
                + "}Ubuntu_16.04-w1-" + ubuntuNodeTemplateId + "-Testing-Freeze_and_Defrost"
                + WineryVersion.WINERY_VERSION_SEPARATOR + WineryVersion.WINERY_VERSION_PREFIX + "1"),
            topology.getNodeTemplate(ubuntuNodeTemplateId).getType()
        );
        assertEquals(
            QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes" + EnhancementUtils.GENERATED_NS_SUFFIX +
                "}MySQL-Database_w1-" + mySqlNodeTemplateId + "-Freeze_and_defrost"
                + WineryVersion.WINERY_VERSION_SEPARATOR + WineryVersion.WINERY_VERSION_PREFIX + "1"),
            topology.getNodeTemplate(mySqlNodeTemplateId).getType()
        );
        assertNotNull(ModelUtilities.getPropertiesKV(topology.getNodeTemplate(ubuntuNodeTemplateId)));
        assertEquals(9, ModelUtilities.getPropertiesKV(topology.getNodeTemplate(ubuntuNodeTemplateId)).size());
        assertNotNull(ModelUtilities.getPropertiesKV(topology.getNodeTemplate(mySqlNodeTemplateId)));
        assertEquals(3, ModelUtilities.getPropertiesKV(topology.getNodeTemplate(mySqlNodeTemplateId)).size());
    }

    @Test
    void getAvailableFeaturesFilteredByDeploymentTechnology() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate serviceTemplate = RepositoryFactory.getRepository()
            .getElement(
                new ServiceTemplateId(
                    // This ST is the same as the other ones only without a annotated deployment technology.
                    // This is required for the integration tests, as the deployment technology is determined by
                    // the resource.
                    QName.valueOf("{http://opentosca.org/add/management/to/instances/servicetemplates}STWithBasicManagementOnly_noDeplTech-w1-wip1")
                )
            );

        Map<String, Map<QName, String>> openStackFeatures =
            EnhancementUtils.getAvailableFeaturesForTopology(serviceTemplate.getTopologyTemplate(), "OpenStackHeat");
        assertEquals(1, openStackFeatures.size());
        Map<QName, String> openStackFeature = openStackFeatures.get("NodeTypeWithDifferentFeatures_w1-wip1_0");
        assertEquals(2, openStackFeature.size());
        assertEquals("scalable AWS and Heat", openStackFeature.get(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}ScaleFeature_AWS-OpenStack-w1-wip1")));
        assertEquals("ping", openStackFeature.get(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}TechIndependentFeature_w1-wip1")));

        Map<String, Map<QName, String>> kubernetesFeatures =
            EnhancementUtils.getAvailableFeaturesForTopology(serviceTemplate.getTopologyTemplate(), "Kubernetes");
        assertEquals(1, kubernetesFeatures.size());
        Map<QName, String> kubernetesFeature = kubernetesFeatures.get("NodeTypeWithDifferentFeatures_w1-wip1_0");
        assertEquals(2, kubernetesFeature.size());
        assertEquals("scalable Kubernetes", kubernetesFeature.get(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}ScaleFeature_Kubernetes-w1-wip1")));
        assertEquals("ping", kubernetesFeature.get(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}TechIndependentFeature_w1-wip1")));

        Map<String, Map<QName, String>> chefFeatures =
            EnhancementUtils.getAvailableFeaturesForTopology(serviceTemplate.getTopologyTemplate(), "Chef");
        assertEquals(1, chefFeatures.size());
        Map<QName, String> chefFeature = chefFeatures.get("NodeTypeWithDifferentFeatures_w1-wip1_0");
        assertEquals(1, chefFeature.size());
        assertEquals("ping", chefFeature.get(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}TechIndependentFeature_w1-wip1")));

    }
}
