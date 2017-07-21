/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Karoline Saatkamp - initial API and implementation
 *******************************************************************************/

package org.eclipse.winery.repository.splitting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.winery.common.ModelUtilities;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.repository.Prefs;
import org.eclipse.winery.repository.backend.Repository;
import org.eclipse.winery.repository.resources.servicetemplates.ServiceTemplateResource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("Needs to be updated to public test cases")
public class SplittingTest {

	private Splitting splitting = new Splitting();
	private TTopologyTemplate topologyTemplate;
	private TTopologyTemplate topologyTemplate2;

	@Before
	public void initialize() throws Exception {
		ServiceTemplateId id = new ServiceTemplateId("http://www.example.org", "ST", false);
		ServiceTemplateId id2 = new ServiceTemplateId("http://opentosca.org/servicetemplates", "FlinkApp_Demo_Small_On_OpenStack", false);

		// initialize the repo for testing
		new Prefs(true);

		assertTrue(Repository.INSTANCE.exists(id));
		assertTrue(Repository.INSTANCE.exists(id2));

		ServiceTemplateResource res = new ServiceTemplateResource(id);
		topologyTemplate = res.getServiceTemplate().getTopologyTemplate();

		topologyTemplate2 = res.getServiceTemplate().getTopologyTemplate();
	}

	@Test
	public void st1HasTwoNodeTemplatesWithoutIncomingHostedOnRelationshipTemplate() throws Exception {
		List<String> expectedIds = Arrays.asList("NT1", "NT1_2");
		List<TNodeTemplate> expectedNodeTemplates = topologyTemplate.getNodeTemplateOrRelationshipTemplate().stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> expectedIds.contains(nt.getId()))
				.collect(Collectors.toList());

		List<TNodeTemplate> nodeTemplatesWithoutIncomingEdges = splitting.getNodeTemplatesWithoutIncomingHostedOnRelationships(topologyTemplate);
		assertEquals(expectedNodeTemplates, nodeTemplatesWithoutIncomingEdges);
	}

	@Test
	public void st1nt1HasCorrectLocationAttribute() throws Exception {
		TNodeTemplate nt1 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1"))
				.findAny()
				.get();

		Optional<String> location = ModelUtilities.getTargetLabel(nt1);
		assertEquals(Optional.of("l1"), location);
	}

	@Test
	public void st1nt2IsTheNodeTemplateWhichPredecessorsHasNoPredecessors() throws Exception {
		TTopologyTemplate topologyTemplate = new TTopologyTemplate();

		TNodeTemplate nt1 = new TNodeTemplate();
		TNodeTemplate nt2 = new TNodeTemplate();
		TNodeTemplate nt3 = new TNodeTemplate();
		nt1.setId("NT1");
		nt2.setId("NT2");
		nt3.setId("NT3");

		TRelationshipTemplate rt = new TRelationshipTemplate();
		TRelationshipTemplate.SourceOrTargetElement targetElement = new TRelationshipTemplate.SourceOrTargetElement();
		targetElement.setRef(nt2);
		rt.setTargetElement(targetElement);
		TRelationshipTemplate.SourceOrTargetElement sourceElement = new TRelationshipTemplate.SourceOrTargetElement();
		sourceElement.setRef(nt1);
		rt.setSourceElement(sourceElement);

		TRelationshipTemplate rt2 = new TRelationshipTemplate();
		TRelationshipTemplate.SourceOrTargetElement targetElement1 = new TRelationshipTemplate.SourceOrTargetElement();
		targetElement1.setRef(nt1);
		rt2.setTargetElement(targetElement1);
		TRelationshipTemplate.SourceOrTargetElement sourceElement1 = new TRelationshipTemplate.SourceOrTargetElement();
		sourceElement1.setRef(nt3);
		rt2.setSourceElement(sourceElement1);

		List<TEntityTemplate> entityTemplates = topologyTemplate.getNodeTemplateOrRelationshipTemplate();
		entityTemplates.add(nt1);
		entityTemplates.add(nt2);
		entityTemplates.add(nt3);
		entityTemplates.add(rt);
		List<TNodeTemplate> expectedNodeTemplates = new ArrayList<>();
		expectedNodeTemplates.add(nt1);
		assertEquals(expectedNodeTemplates, splitting.getNodeTemplatesWhichPredecessorsHasNoPredecessors(topologyTemplate));
	}

	@Test
	public void testgetSuccessor() throws Exception {
		TNodeTemplate nt1 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1"))
				.findAny()
				.get();

		TNodeTemplate nt2 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_3"))
				.findAny()
				.get();

		List<TNodeTemplate> expectedNodes = new ArrayList<>();
		expectedNodes.add(nt2);

		assertEquals(expectedNodes, splitting.getHostedOnSuccessorsOfNodeTemplate(topologyTemplate2, nt1));
	}

	@Test
	public void testcheckValidationTopologyForAValidTopology() throws Exception {
		TNodeTemplate nt1 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1"))
				.findAny()
				.get();

		ModelUtilities.setTargetLabel(nt1, "1");

		TNodeTemplate nt2 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_3"))
				.findAny()
				.get();
		ModelUtilities.setTargetLabel(nt2, "1");

		TNodeTemplate nt3 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_2"))
				.findAny()
				.get();
		ModelUtilities.setTargetLabel(nt3, "1");

		assertEquals(true, splitting.checkValidTopology(topologyTemplate));
	}

	@Test
	public void testcheckValidationTopologyForAInValidTopology() throws Exception {
		TNodeTemplate nt1 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1"))
				.findAny()
				.get();

		ModelUtilities.setTargetLabel(nt1, "1");

		TNodeTemplate nt2 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_4"))
				.findAny()
				.get();
		ModelUtilities.setTargetLabel(nt2, "2");


		TNodeTemplate nt3 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_2"))
				.findAny()
				.get();
		ModelUtilities.setTargetLabel(nt3, "1");

		assertEquals(false, splitting.checkValidTopology(topologyTemplate));
	}

	@Test
	public void testcheckValidationTopologyForAValidTopologyWithSuccessorHasEmptyTargetLabel() throws Exception {
		TNodeTemplate nt1 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1"))
				.findAny()
				.get();

		ModelUtilities.setTargetLabel(nt1, "1");

		TNodeTemplate nt3 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_2"))
				.findAny()
				.get();
		ModelUtilities.setTargetLabel(nt3, "2");

		assertEquals(true, splitting.checkValidTopology(topologyTemplate));
	}

	@Test
	public void testcheckValidationTopologyForAInValidTopologyWithFirstNodesHasEmptyTargetLabel() throws Exception {

		TNodeTemplate nt3 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_2"))
				.findAny()
				.get();
		ModelUtilities.setTargetLabel(nt3, "2");

		assertEquals(false, splitting.checkValidTopology(topologyTemplate));
	}

	@Test
	public void testgetPredecessorsWhichPredecessorsHasNoPredecessors() {
		TNodeTemplate nt1 = topologyTemplate.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.filter(nt -> nt.getId().equals("NT1_3"))
				.findAny()
				.get();

		assertEquals(nt1.getId(), splitting.getNodeTemplatesWhichPredecessorsHasNoPredecessors(topologyTemplate).get(0).getId());
	}

	@Test
	public void splitSmallTopology() throws Exception {
		List<String> expectedIds = Arrays.asList("NT1", "NT1_2", "NT1_3-A", "NT1_3-B", "NT1_4-A", "NT1_4-B", "NT1_5-A", "NT1_5-B", "con37", "con45", "con_91-A-A", "con_57-B-B");
		List<TEntityTemplate> NodeTemplates = splitting.split(topologyTemplate).getNodeTemplateOrRelationshipTemplate();

		List<String> Ids = new ArrayList<>();
		for (TEntityTemplate nodeTemplate : NodeTemplates) {
			Ids.add(nodeTemplate.getId());
		}

		assertEquals(expectedIds, Ids);
	}

	@Test
	public void testmatchingofSplittingTopology() throws Exception {
		ServiceTemplateId serviceTemplateId = new ServiceTemplateId("http://opentosca.org/servicetemplates", "Abstract_PHPApp_MySQLDB_MotivatingScenario_Splitting-split", false);

		// initialize the repo for testing
		new Prefs(true);

		assertTrue(Repository.INSTANCE.exists(serviceTemplateId));

		ServiceTemplateResource resource = new ServiceTemplateResource(serviceTemplateId);
		TTopologyTemplate topologyTemplateMatching = resource.getServiceTemplate().getTopologyTemplate();

		List<String> expectedIds = Arrays.asList("PHP-5-WebApplication", "Java7", "MySQL-DB", "PHP-5-Module", "Apache-2.4", "Ubuntu-14.04-VM-OnPremiseIAAS", "OpenStack-Liberty-12-OnPremiseIAAS", "AmazonBeanstalk", "AmazonRDS");
		List<TNodeTemplate> NodeTemplates = splitting.matchingWithDefaultHostSelection(topologyTemplateMatching).getNodeTemplateOrRelationshipTemplate().stream().filter(t -> t instanceof TNodeTemplate).map(TNodeTemplate.class::cast).collect(Collectors.toList());

		List<String> Ids = new ArrayList<>();
		for (TNodeTemplate nodeTemplate : NodeTemplates) {
			Ids.add(nodeTemplate.getId());
		}

		assertEquals(expectedIds, Ids);
	}


}
