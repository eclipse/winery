/*******************************************************************************
 * Copyright (c) 2020-2021 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.edmm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.edmm.model.EdmmType;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TImplementationArtifact;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.extensions.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;

import io.github.edmm.model.component.Compute;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.component.SoftwareComponent;
import io.github.edmm.model.component.WebApplication;
import io.github.edmm.model.relation.ConnectsTo;
import io.github.edmm.model.relation.DependsOn;
import io.github.edmm.model.relation.HostedOn;
import org.junit.jupiter.api.BeforeEach;

public abstract class EdmmDependantTest {

    protected final String NAMESPACE = "https://ex.org/tosca/to/edmm";
    protected final String NAMESPACE_DOUBLE_ENCODED = URLEncoder.encode(URLEncoder.encode(NAMESPACE, "UTF-8"), "UTF-8");
    protected final HashMap<QName, TNodeType> nodeTypes = new HashMap<>();
    protected final HashMap<QName, TRelationshipType> relationshipTypes = new HashMap<>();
    protected final HashMap<String, TNodeTemplate> nodeTemplates = new HashMap<>();
    protected final HashMap<String, TRelationshipTemplate> relationshipTemplates = new HashMap<>();
    protected final HashMap<QName, TNodeTypeImplementation> nodeTypeImplementations = new HashMap<>();
    protected final HashMap<QName, TRelationshipTypeImplementation> relationshipTypeImplementations = new HashMap<>();
    protected final HashMap<QName, TArtifactTemplate> artifactTemplates = new HashMap<>();
    protected final HashMap<QName, EdmmType> edmm1to1Mapping = new HashMap<>();

    protected EdmmDependantTest() throws UnsupportedEncodingException {
    }

    @BeforeEach
    void setup() {
        // region *** NodeType setup ***
        QName nodeTypeRootQName = QName.valueOf("{" + NAMESPACE + "}" + "root-node-type");
        TNodeType rootNodeType = new TNodeType();
        rootNodeType.setName(nodeTypeRootQName.getLocalPart());
        rootNodeType.setTargetNamespace(nodeTypeRootQName.getNamespaceURI());
        nodeTypes.put(nodeTypeRootQName, rootNodeType);

        QName nodeTypeComputeQName = QName.valueOf("{" + NAMESPACE + "}" + "compute-node-type");
        TNodeType computeNodeType = new TNodeType();
        computeNodeType.setName(nodeTypeComputeQName.getLocalPart());
        computeNodeType.setTargetNamespace(nodeTypeComputeQName.getNamespaceURI());
        TEntityType.DerivedFrom parentOfCompute = new TNodeType.DerivedFrom();
        parentOfCompute.setTypeRef(nodeTypeRootQName);
        computeNodeType.setDerivedFrom(parentOfCompute);
        nodeTypes.put(nodeTypeComputeQName, computeNodeType);

        QName nodeTypeWebApplicationQName = QName.valueOf("{" + NAMESPACE + "}" + "web-application-node-type");
        TNodeType webApplicationNodeType = new TNodeType();
        webApplicationNodeType.setName(nodeTypeWebApplicationQName.getLocalPart());
        webApplicationNodeType.setTargetNamespace(nodeTypeWebApplicationQName.getNamespaceURI());
        TEntityType.DerivedFrom parentOfWebApplication = new TNodeType.DerivedFrom();
        parentOfWebApplication.setTypeRef(nodeTypeRootQName);
        webApplicationNodeType.setDerivedFrom(parentOfWebApplication);
        nodeTypes.put(nodeTypeWebApplicationQName, webApplicationNodeType);

        QName nodeTypeSoftwareComponentQName = QName.valueOf("{" + NAMESPACE + "}" + "software-component-node-type");
        TNodeType softwareComponentNodeType = new TNodeType();
        softwareComponentNodeType.setName(nodeTypeSoftwareComponentQName.getLocalPart());
        softwareComponentNodeType.setTargetNamespace(nodeTypeSoftwareComponentQName.getNamespaceURI());
        TEntityType.DerivedFrom parentOfSoftwareComponent = new TNodeType.DerivedFrom();
        parentOfSoftwareComponent.setTypeRef(nodeTypeRootQName);
        softwareComponentNodeType.setDerivedFrom(parentOfSoftwareComponent);
        nodeTypes.put(nodeTypeSoftwareComponentQName, softwareComponentNodeType);

        QName nodeType1QName = QName.valueOf("{" + NAMESPACE + "}" + "test_node_type");
        TNodeType nodeType1 = new TNodeType();
        nodeType1.setName(nodeType1QName.getLocalPart());
        nodeType1.setTargetNamespace(nodeType1QName.getNamespaceURI());
        TEntityType.DerivedFrom parentForNodeType1 = new TNodeType.DerivedFrom();
        parentForNodeType1.setTypeRef(nodeTypeRootQName);
        nodeType1.setDerivedFrom(parentForNodeType1);
        nodeTypes.put(nodeType1QName, nodeType1);

        QName nodeType2QName = QName.valueOf("{" + NAMESPACE + "}" + "test_node_type_2");
        TNodeType nodeType2 = new TNodeType();
        nodeType2.setName(nodeType2QName.getLocalPart());
        nodeType2.setTargetNamespace(nodeType2QName.getNamespaceURI());
        TEntityType.DerivedFrom derivedFrom = new TNodeType.DerivedFrom();
        derivedFrom.setTypeRef(nodeTypeSoftwareComponentQName);
        nodeType2.setDerivedFrom(derivedFrom);
        nodeTypes.put(nodeType2QName, nodeType2);

        QName nodeType3QName = QName.valueOf("{" + NAMESPACE + "}" + "test_node_type_3");
        TNodeType nodeType3 = new TNodeType();
        nodeType3.setName(nodeType3QName.getLocalPart());
        nodeType3.setTargetNamespace(nodeType3QName.getNamespaceURI());
        List<PropertyDefinitionKV> kvList = new ArrayList<>(Arrays.asList(
            new PropertyDefinitionKV("os_family", "xsd:string"),
            new PropertyDefinitionKV("public_key", "xsd:string"),
            new PropertyDefinitionKV("ssh_port", "number")
        ));
        WinerysPropertiesDefinition wpd = new WinerysPropertiesDefinition();
        wpd.setPropertyDefinitions(kvList);
        ModelUtilities.replaceWinerysPropertiesDefinition(nodeType3, wpd);
        nodeType3.setProperties(wpd);
        TEntityType.DerivedFrom parentOfNodeType3 = new TNodeType.DerivedFrom();
        parentOfNodeType3.setTypeRef(nodeTypeComputeQName);
        nodeType3.setDerivedFrom(parentOfNodeType3);
        nodeTypes.put(nodeType3QName, nodeType3);

        QName nodeType4QName = QName.valueOf("{" + NAMESPACE + "}" + "test_node_type_4");
        TNodeType nodeType4 = new TNodeType();
        nodeType4.setName(nodeType4QName.getLocalPart());
        nodeType4.setTargetNamespace(nodeType4QName.getNamespaceURI());
        TOperation start = new TOperation();
        start.setName("start");
        TOperation stop = new TOperation();
        stop.setName("stop");
        TInterface lifecycle = new TInterface();
        lifecycle.setName("lifecycle_interface");
        lifecycle.getOperations().add(start);
        lifecycle.getOperations().add(stop);
        List<TInterface> tInterfaces = new ArrayList<>();
        tInterfaces.add(lifecycle);
        nodeType4.setInterfaces(tInterfaces);
        TEntityType.DerivedFrom parentOfNodeType4 = new TNodeType.DerivedFrom();
        parentOfNodeType4.setTypeRef(nodeTypeWebApplicationQName);
        nodeType4.setDerivedFrom(parentOfNodeType4);
        nodeTypes.put(nodeType4QName, nodeType4);
        // endregion

        // region *** ArtifactTemplates setup ***
        QName startIaQName = QName.valueOf("{" + NAMESPACE + "}" + "Start_IA");
        artifactTemplates.put(startIaQName,
            new TArtifactTemplate.Builder(
                "Start_IA",
                startIaQName
            )
                .addArtifactReference(
                    new TArtifactReference.Builder(
                        "/artifacttemplates/" + NAMESPACE_DOUBLE_ENCODED + "/startTestNode4/files/script.sh"
                    ).build()
                )
                .build()
        );

        QName stopIaQName = QName.valueOf("{" + NAMESPACE + "}" + "Stop_IA");
        TArtifactTemplate stopIa = new TArtifactTemplate.Builder(
            "Stop_IA",
            stopIaQName
        )
            .addArtifactReference(
                new TArtifactReference.Builder(
                    "/artifacttemplates/" + NAMESPACE_DOUBLE_ENCODED + "/startTestNode4/files/script.sh"
                ).build()
            )
            .build();
        artifactTemplates.put(stopIaQName, stopIa);

        QName deploymentArtifactIAQName = QName.valueOf("{" + NAMESPACE + "}" + "TestNode1-DA");
        TArtifactTemplate deploymentArtifactTemplate = new TArtifactTemplate.Builder(
            "TestNode1-DA",
            deploymentArtifactIAQName
        )
            .addArtifactReference(
                new TArtifactReference.Builder(
                    "/artifacttemplates/" + NAMESPACE_DOUBLE_ENCODED + "/testNode1-DA/files/da.war"
                ).build()
            )
            .build();
        artifactTemplates.put(deploymentArtifactIAQName, deploymentArtifactTemplate);
        // endregion

        // region *** NodeTypeImplementations setup ***
        QName nodeTypeImpl4QName = QName.valueOf("{" + NAMESPACE + "}" + "test_node_type_Impl_4");

        TImplementationArtifact startArtifact = new TImplementationArtifact.Builder(QName.valueOf("{ex.org}test"))
            .setArtifactRef(startIaQName)
            .setInterfaceName("lifecycle_interface")
            .setOperationName("start")
            .build();
        TImplementationArtifact stopArtifact = new TImplementationArtifact.Builder(QName.valueOf("{ex.org}test"))
            .setArtifactRef(stopIaQName)
            .setInterfaceName("lifecycle_interface")
            .setOperationName("stop")
            .build();

        nodeTypeImplementations.put(
            nodeTypeImpl4QName,
            new TNodeTypeImplementation.Builder(nodeTypeImpl4QName.getLocalPart(), nodeType4QName)
                .addImplementationArtifact(startArtifact)
                .addImplementationArtifact(stopArtifact)
                .build()
        );

        // endregion

        QName dependsOnQName = QName.valueOf("{" + NAMESPACE + "}" + "dependsOn");
        TRelationshipType dependsOnType = new TRelationshipType();
        dependsOnType.setName(dependsOnQName.getLocalPart());
        dependsOnType.setTargetNamespace(dependsOnQName.getNamespaceURI());
        relationshipTypes.put(dependsOnQName, dependsOnType);

        // region *** RelationType setup ***
        QName hostedOnQName = QName.valueOf("{" + NAMESPACE + "}" + "hostedOn");
        TRelationshipType hostedOnType = new TRelationshipType();
        hostedOnType.setName(hostedOnQName.getLocalPart());
        hostedOnType.setTargetNamespace(hostedOnQName.getNamespaceURI());
        TEntityType.DerivedFrom hostedOnDerivedFrom = new TRelationshipType.DerivedFrom();
        hostedOnDerivedFrom.setTypeRef(dependsOnQName);
        hostedOnType.setDerivedFrom(hostedOnDerivedFrom);
        relationshipTypes.put(hostedOnQName, hostedOnType);

        QName connectsToQName = QName.valueOf("{" + NAMESPACE + "}" + "connectsTo");
        TRelationshipType connectsToType = new TRelationshipType();
        connectsToType.setName(connectsToQName.getLocalPart());
        connectsToType.setTargetNamespace(connectsToQName.getNamespaceURI());
        TEntityType.DerivedFrom connectsToDerivedFrom = new TRelationshipType.DerivedFrom();
        connectsToDerivedFrom.setTypeRef(dependsOnQName);
        connectsToType.setDerivedFrom(connectsToDerivedFrom);
        relationshipTypes.put(connectsToQName, connectsToType);
        // endregion

        // region *** create NodeTemplates ***
        TDeploymentArtifact artifact = new TDeploymentArtifact.Builder(
            "test_artifact", QName.valueOf("{" + NAMESPACE + "}" + "WAR")
        )
            .setArtifactRef(deploymentArtifactIAQName)
            .build();

        TNodeTemplate nt1 = new TNodeTemplate.Builder("test_node_1", nodeType1QName)
            .setName("test_node_1")
            .addDeploymentArtifact(artifact)
            .build();
        nodeTemplates.put(nt1.getId(), nt1);

        TNodeTemplate nt2 = new TNodeTemplate();
        nt2.setType(nodeType2QName);
        nt2.setId("test_node_2");
        nt2.setName("test_node_2");
        nodeTemplates.put(nt2.getId(), nt2);

        TNodeTemplate nt3 = new TNodeTemplate();
        nt3.setType(nodeType3QName);
        nt3.setId("test_node_3");
        nt3.setName("test_node_3");
        TEntityTemplate.WineryKVProperties properties = new TEntityTemplate.WineryKVProperties();
        LinkedHashMap<String, String> nt3Properties = new LinkedHashMap<>();
        nt3Properties.put("os_family", "ubuntu");
        nt3Properties.put("public_key", "-----BEGIN PUBLIC KEY----- ... -----END PUBLIC KEY-----");
        nt3Properties.put("ssh_port", "22");
        properties.setKVProperties(nt3Properties);
        nt3.setProperties(properties);
        nodeTemplates.put(nt3.getId(), nt3);

        TNodeTemplate nt4 = new TNodeTemplate();
        nt4.setType(nodeType4QName);
        nt4.setId("test_node_4");
        nt4.setName("test_node_4");
        nodeTemplates.put(nt4.getId(), nt4);
        // endregion 

        // region *** create RelationshipTemplate ***
        TRelationshipTemplate rt13 = new TRelationshipTemplate();
        rt13.setType(hostedOnQName);
        rt13.setId("1_hosted_on_3");
        rt13.setName("1_hosted_on_3");
        rt13.setSourceNodeTemplate(nt1);
        rt13.setTargetNodeTemplate(nt3);
        relationshipTemplates.put(rt13.getId(), rt13);

        TRelationshipTemplate rt23 = new TRelationshipTemplate();
        rt23.setType(hostedOnQName);
        rt23.setId("2_hosted_on_3");
        rt23.setName("2_hosted_on_3");
        rt23.setSourceNodeTemplate(nt2);
        rt23.setTargetNodeTemplate(nt3);
        relationshipTemplates.put(rt23.getId(), rt23);

        TRelationshipTemplate rt41 = new TRelationshipTemplate();
        rt41.setType(hostedOnQName);
        rt41.setId("4_hosted_on_1");
        rt41.setName("4_hosted_on_1");
        rt41.setSourceNodeTemplate(nt4);
        rt41.setTargetNodeTemplate(nt1);
        relationshipTemplates.put(rt41.getId(), rt41);

        TRelationshipTemplate rt12 = new TRelationshipTemplate();
        rt12.setType(connectsToQName);
        rt12.setId("1_connects_to_2");
        rt12.setName("1_connects_to_2");
        rt12.setSourceNodeTemplate(nt1);
        rt12.setTargetNodeTemplate(nt2);
        relationshipTemplates.put(rt12.getId(), rt12);
        // endregion

        // region *** create edmm type mapping ***
        EdmmType customMadeEdmmType = new EdmmType("great_type");
        edmm1to1Mapping.put(nodeType1QName, customMadeEdmmType);
        // edmmTypeMapping.put(nodeType2QName, EdmmType.SOFTWARE_COMPONENT);
        // edmmTypeExtendsMapping.put(nodeType3QName, EdmmType.fromEntityClass(Compute.class));
        // edmmTypeExtendsMapping.put(nodeType4QName, EdmmType.fromEntityClass(WebApplication.class));
        edmm1to1Mapping.put(hostedOnQName, EdmmType.fromEntityClass(HostedOn.class));
        edmm1to1Mapping.put(connectsToQName, EdmmType.fromEntityClass(ConnectsTo.class));
        edmm1to1Mapping.put(dependsOnQName, EdmmType.fromEntityClass(DependsOn.class));
        edmm1to1Mapping.put(nodeTypeRootQName, EdmmType.fromEntityClass(RootComponent.class));
        edmm1to1Mapping.put(nodeTypeComputeQName, EdmmType.fromEntityClass(Compute.class));
        edmm1to1Mapping.put(nodeTypeSoftwareComponentQName, EdmmType.fromEntityClass(SoftwareComponent.class));
        edmm1to1Mapping.put(nodeTypeWebApplicationQName, EdmmType.fromEntityClass(WebApplication.class));
        // endregion
    }
}
