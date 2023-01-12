/*******************************************************************************
 * Copyright (c) 2017-2022 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.splitting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.ids.definitions.CapabilityTypeId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.RequirementTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TCapabilityType;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.constants.OpenToscaBaseTypes;
import org.eclipse.winery.model.tosca.constants.ToscaBaseTypes;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.model.version.VersionSupport;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.common.Util;
import org.eclipse.winery.repository.driverspecificationandinjection.DASpecification;
import org.eclipse.winery.repository.driverspecificationandinjection.DriverInjection;

import org.slf4j.LoggerFactory;

import static org.eclipse.winery.common.ListUtils.listIsNotNullOrEmpty;

public class Splitting {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Splitting.class);

    // counter for relationships starts at 100 because all TRelationshipTemplate should have a 3-digit number in their id
    private static int newRelationshipIdCounter = 100;
    private static int idCounter = 1;
    private static int newCapabilityCounter = 1;

    // Required variables for the following computation of the transitive closure of a given topology
    private Map<TNodeTemplate, Set<TNodeTemplate>> initDirectSuccessors = new HashMap<>();
    private Map<TNodeTemplate, Boolean> visitedNodeTemplates = new HashMap<>();
    private Map<TNodeTemplate, Set<TNodeTemplate>> transitiveAndDirectSuccessors = new HashMap<>();

    /**
     * Splits the topology template of the given service template. Creates a new service template with "-split" suffix
     * as id. Any existing "-split" service template will be deleted. Matches the split topology template to the cloud
     * providers according to the target labels. Creates a new service template with "-matched" suffix as id. Any
     * existing "-matched" service template will be deleted.
     *
     * @param id of the ServiceTemplate switch should be split and matched to cloud providers
     * @return id of the ServiceTemplate which contains the matched topology
     */
    public ServiceTemplateId splitTopologyOfServiceTemplate(ServiceTemplateId id) throws SplittingException, IOException {
        IRepository repository = RepositoryFactory.getRepository();
        TServiceTemplate serviceTemplate = repository.getElement(id);

        // create wrapper service template
        ServiceTemplateId splitServiceTemplateId = new ServiceTemplateId(
            id.getNamespace().getDecoded(),
            VersionSupport.getNewComponentVersionId(id, "split"),
            false);

        repository.forceDelete(splitServiceTemplateId);
        repository.flagAsExisting(splitServiceTemplateId);
        TServiceTemplate splitServiceTemplate = new TServiceTemplate();
        splitServiceTemplate.setName(splitServiceTemplateId.getXmlId().getDecoded());
        splitServiceTemplate.setId(splitServiceTemplate.getName());
        splitServiceTemplate.setTargetNamespace(id.getNamespace().getDecoded());
        splitServiceTemplate.setTags(serviceTemplate.getTags());
        TTopologyTemplate splitTopologyTemplate = split(serviceTemplate).getTopologyTemplate();
        splitServiceTemplate.setTopologyTemplate(splitTopologyTemplate);

        LOGGER.debug("Persisting...");
        repository.setElement(splitServiceTemplateId, splitServiceTemplate);
        LOGGER.debug("Persisted.");

        return splitServiceTemplateId;
    }

    public ServiceTemplateId splitAndMatchTopologyOfServiceTemplate(ServiceTemplateId id) throws
        SplittingException, IOException {

        ServiceTemplateId splitServiceTemplateId = splitTopologyOfServiceTemplate(id);

        ServiceTemplateId matchedServiceTemplateId = new ServiceTemplateId(
            id.getNamespace().getDecoded(),
            VersionSupport.getNewComponentVersionId(id, "split-matched"),
            false);

        IRepository repository = RepositoryFactory.getRepository();
        repository.forceDelete(matchedServiceTemplateId);
        repository.flagAsExisting(matchedServiceTemplateId);
        TServiceTemplate matchedServiceTemplate = new TServiceTemplate();
        matchedServiceTemplate.setName(matchedServiceTemplateId.getXmlId().getDecoded());
        matchedServiceTemplate.setId(matchedServiceTemplate.getName());
        matchedServiceTemplate.setTargetNamespace(id.getNamespace().getDecoded());
        TTopologyTemplate matchedTopologyTemplate = hostMatchingWithDefaultHostSelection(repository.getElement(splitServiceTemplateId));
        matchedServiceTemplate.setTopologyTemplate(matchedTopologyTemplate);
        LOGGER.debug("Persisting...");
        repository.setElement(matchedServiceTemplateId, matchedServiceTemplate);
        LOGGER.debug("Persisted.");
        return matchedServiceTemplateId;
    }

    /**
     * Splits the topology template of the given service template. Creates a new service template with "-split" suffix
     * as id. Any existing "-split" service template will be deleted. Matches the split topology template to the cloud
     * providers according to the target labels. Creates a new service template with "-matched" suffix as id. Any
     * existing "-matched" service template will be deleted.
     *
     * @param id of the ServiceTemplate switch should be split and matched to cloud providers
     * @return id of the ServiceTemplate which contains the matched topology
     */
    public ServiceTemplateId matchTopologyOfServiceTemplate(ServiceTemplateId id) throws Exception {
        long start = System.currentTimeMillis();
        IRepository repository = RepositoryFactory.getRepository();

        TServiceTemplate serviceTemplate = repository.getElement(id);
        TTopologyTemplate topologyTemplate = serviceTemplate.getTopologyTemplate();

        /*
        Get all open requirements and the basis type of the required capability type
        Two different basis types are distinguished:
            "Container" which means a hostedOn injection is required
            "Endpoint" which means a connectsTo injection is required
        */
        Map<TRequirement, String> requirementsAndMatchingBasisCapabilityTypes =
            getOpenRequirementsAndMatchingBasisCapabilityTypeNames(serviceTemplate);
        // Output check
        LOGGER.debug("Matching for ServiceTemplate with ID: {}", id.getQName());
        for (TRequirement req : requirementsAndMatchingBasisCapabilityTypes.keySet()) {
            LOGGER.debug("Open Requirement: {}", req.getId());
            LOGGER.debug("Matching basis type: {}", requirementsAndMatchingBasisCapabilityTypes.get(req));
        }

        // create wrapper service template
        ServiceTemplateId matchedServiceTemplateId = new ServiceTemplateId(
            id.getNamespace().getDecoded(),
            VersionSupport.getNewComponentVersionId(id, "matched"),
            false);
        RepositoryFactory.getRepository().forceDelete(matchedServiceTemplateId);
        RepositoryFactory.getRepository().flagAsExisting(matchedServiceTemplateId);
        repository.flagAsExisting(matchedServiceTemplateId);
        TServiceTemplate matchedServiceTemplate = BackendUtils.clone(serviceTemplate);
        matchedServiceTemplate.setName(matchedServiceTemplateId.getXmlId().getDecoded());
        matchedServiceTemplate.setId(matchedServiceTemplate.getName());
        matchedServiceTemplate.setTargetNamespace(id.getNamespace().getDecoded());
        matchedServiceTemplate.setTags(serviceTemplate.getTags());
        TTopologyTemplate matchedConnectedTopologyTemplate;
        if (requirementsAndMatchingBasisCapabilityTypes.containsValue("Container")) {
            // set default target labels if they are not yet set
            if (!checkApplicationSpecificComponentTargetLabeling(serviceTemplate)) {
                LOGGER.debug("Target labels are not set for all NodeTemplates. Using default target labels.");
                topologyTemplate.getNodeTemplates().forEach(t -> ModelUtilities.setTargetLabel(t, "*"));
            }

            matchedServiceTemplate.setTopologyTemplate(hostMatchingWithDefaultHostSelection(matchedServiceTemplate));

            if (requirementsAndMatchingBasisCapabilityTypes.containsValue("Endpoint")) {
                matchedServiceTemplate.setTopologyTemplate(connectionMatchingWithDefaultConnectorSelection(matchedServiceTemplate));
            }
        } else if (requirementsAndMatchingBasisCapabilityTypes.containsValue("Endpoint")) {
            matchedServiceTemplate.setTopologyTemplate(connectionMatchingWithDefaultConnectorSelection(matchedServiceTemplate));
        } else {
            throw new SplittingException("No open Requirements which can be matched");
        }

        TTopologyTemplate daSpecifiedTopology = matchedServiceTemplate.getTopologyTemplate();

        //Start additional functionality Driver Injection
        if (!DASpecification.getNodeTemplatesWithAbstractDAs(daSpecifiedTopology).isEmpty() &&
            DASpecification.getNodeTemplatesWithAbstractDAs(daSpecifiedTopology) != null) {
            daSpecifiedTopology = DriverInjection.injectDriver(daSpecifiedTopology);
        }
        //End additional functionality Driver Injection

        matchedServiceTemplate.setTopologyTemplate(daSpecifiedTopology);
        LOGGER.debug("Persisting...");
        repository.setElement(matchedServiceTemplateId, matchedServiceTemplate);
        LOGGER.debug("Persisted.");

        long duration = System.currentTimeMillis() - start;
        LOGGER.debug("Execution Time in millisec: " + duration + "ms");

        return matchedServiceTemplateId;
    }

    /**
     * s Check if the TopologyTemplate contains NodeTemplates without a set target label.
     *
     * @param topologyTemplate the TopologyTemplate to check
     * @return <code>true</code> if all contained NodeTemplates have a target label set, <code>false</code> otherwise
     */
    private boolean hasTargetLabels(TTopologyTemplate topologyTemplate) {
        return topologyTemplate.getNodeTemplates().stream()
            .allMatch(node -> Objects.nonNull(node.getOtherAttributes())
                && Objects.nonNull(node.getOtherAttributes().get(ModelUtilities.QNAME_LOCATION)));
    }

    public TNodeType createPlaceholderNodeType(String nameOfNodeTemplateGettingPlaceholder) {
        TNodeType placeholderNodeType = new TNodeType();
        placeholderNodeType.setName(nameOfNodeTemplateGettingPlaceholder + "_placeholder");
        placeholderNodeType.setId(nameOfNodeTemplateGettingPlaceholder + "_placeholder");
        placeholderNodeType.setTargetNamespace(OpenToscaBaseTypes.placeholderTypeNamespace);

        return placeholderNodeType;
    }

    public TNodeTemplate createPlaceholderNodeTemplate(TTopologyTemplate topologyTemplate, TNodeTemplate
        NodeTemplateGettingPlaceholder, QName placeholderQName) {
        TNodeTemplate placeholderNodeTemplate = new TNodeTemplate();
        StringBuilder id;
        List<String> ids = new ArrayList<>();
        for (TNodeTemplate nt : topologyTemplate.getNodeTemplates()) {
            ids.add(nt.getId());
        }
        boolean uniqueID = false;
        id = new StringBuilder(NodeTemplateGettingPlaceholder.getName() + "_placeholder");
        while (!uniqueID) {
            if (!ids.contains(id.toString() + idCounter)) {
                id.append(idCounter);
                idCounter++;
                uniqueID = true;
            } else {
                idCounter++;
            }
        }
        placeholderNodeTemplate.setId(id.toString());
        placeholderNodeTemplate.setName(id.toString());
        placeholderNodeTemplate.setType(placeholderQName);

        return placeholderNodeTemplate;
    }

    public List<TParameter> getInputParamListofIncomingRelationshipTemplate(TTopologyTemplate
                                                                                topologyTemplate, TRelationshipTemplate incomingRelationshipTemplate) {
        List<TParameter> listOfInputs = new ArrayList<>();
        IRepository repo = RepositoryFactory.getRepository();
        TNodeTemplate incomingNodetemplate = ModelUtilities.getSourceNodeTemplateOfRelationshipTemplate(topologyTemplate, incomingRelationshipTemplate);
        NodeTypeId incomingNodeTypeId = new NodeTypeId(incomingNodetemplate.getType());
        TNodeType incomingNodeType = repo.getElement(incomingNodeTypeId);
        List<TInterface> incomingNodeTypeInterfaces = incomingNodeType.getInterfaces();
        RelationshipTypeId incomingRelationshipTypeId = new RelationshipTypeId(incomingRelationshipTemplate.getType());

        if (incomingNodeTypeInterfaces != null && !incomingNodeTypeInterfaces.isEmpty()) {
            TInterface relevantInterface = null;
            List<TInterface> connectionInterfaces = incomingNodeTypeInterfaces.stream().filter(tInterface -> tInterface.getIdFromIdOrNameField().contains("connect")).collect(Collectors.toList());
            if (connectionInterfaces.size() > 1) {
                TNodeTemplate targetNodeTemplate = ModelUtilities.getTargetNodeTemplateOfRelationshipTemplate(topologyTemplate, incomingRelationshipTemplate);
                for (TInterface tInterface : connectionInterfaces) {
                    int separator = tInterface.getIdFromIdOrNameField().lastIndexOf("/");
                    String prefixRelation = tInterface.getIdFromIdOrNameField().substring(separator + 1);
                    if (targetNodeTemplate.getName() != null && targetNodeTemplate.getName().toLowerCase().contains(prefixRelation.toLowerCase())) {
                        relevantInterface = tInterface;
                    }
                }
            } else if (connectionInterfaces.size() == 1) {
                relevantInterface = connectionInterfaces.get(0);
            }

            if (relevantInterface != null) {
                for (TOperation tOperation : relevantInterface.getOperations()) {
                    List<TParameter> inputParameters = tOperation.getInputParameters();
                    if (inputParameters != null) {
                        listOfInputs.addAll(inputParameters);
                    }
                }
            }
        }
        return listOfInputs;
    }

    public TCapability createPlaceholderCapability(TTopologyTemplate topologyTemplate, QName capabilityType) {
        // unique id for capability
        String id;
        List<String> ids = new ArrayList<>();
        for (TNodeTemplate nt : topologyTemplate.getNodeTemplates()) {
            if (nt.getCapabilities() != null) {
                nt.getCapabilities().forEach(cap -> ids.add(cap.getId()));
            }
        }
        boolean uniqueID = false;
        id = "0";
        while (!uniqueID) {
            if (!ids.contains("cap_" + newCapabilityCounter)) {
                id = "cap_" + newCapabilityCounter;
                newCapabilityCounter++;
                uniqueID = true;
            } else {
                newCapabilityCounter++;
            }
        }

        return new TCapability.Builder(id, capabilityType, id).build();
    }

    /**
     *
     */
    public ServiceTemplateId composeServiceTemplates(String
                                                         composedSolutionServiceTemplateID, List<ServiceTemplateId> serviceTemplateIds) throws
        IOException, SplittingException {
        IRepository repository = RepositoryFactory.getRepository();
        String solutionNamespace = "http://www.opentosca.org/solutions/";

        // create composed service template
        ServiceTemplateId composedServiceTemplateId =
            new ServiceTemplateId(solutionNamespace, composedSolutionServiceTemplateID, false);
        repository.forceDelete(composedServiceTemplateId);
        repository.flagAsExisting(composedServiceTemplateId);
        TServiceTemplate composedServiceTemplate = new TServiceTemplate();
        composedServiceTemplate.setName(composedServiceTemplateId.getXmlId().getDecoded());
        composedServiceTemplate.setId(composedServiceTemplate.getName());
        composedServiceTemplate.setTargetNamespace(solutionNamespace);
        TTopologyTemplate composedTopologyTemplate = new TTopologyTemplate();
        composedServiceTemplate.setTopologyTemplate(composedTopologyTemplate);
        repository.setElement(composedServiceTemplateId, composedServiceTemplate);
        //add all node and relationship templates from the solution fragements to the composed topology template
        for (ServiceTemplateId id : serviceTemplateIds) {
            BackendUtils.mergeTopologyTemplateAinTopologyTemplateB(id, composedServiceTemplateId, repository);
        }
        composedServiceTemplate = repository.getElement(composedServiceTemplateId);
        composedTopologyTemplate = composedServiceTemplate.getTopologyTemplate();
        List<TRequirement> openRequirements = getOpenRequirements(composedServiceTemplate);
        for (TRequirement requirement : openRequirements) {
            QName requiredCapabilityTypeQName = getRequiredCapabilityTypeQNameOfRequirement(requirement);

            TNodeTemplate nodeWithOpenCapability = composedTopologyTemplate.getNodeTemplates().stream()
                .filter(nt -> nt.getCapabilities() != null)
                .filter(nt -> nt.getCapabilities().stream()
                    .anyMatch(c -> c.getType().equals(requiredCapabilityTypeQName))).findFirst().orElse(null);

            if (nodeWithOpenCapability != null) {
                TCapability matchingCapability = nodeWithOpenCapability.getCapabilities()
                    .stream().filter(c -> c.getType().equals(requiredCapabilityTypeQName)).findFirst().get();
                TRelationshipType matchingRelationshipType =
                    getMatchingRelationshipType(requirement, matchingCapability);
                if (matchingRelationshipType != null) {
                    addMatchingRelationshipTemplateToTopologyTemplate(composedTopologyTemplate, matchingRelationshipType, requirement, matchingCapability);
                } else {
                    throw new SplittingException("No suitable relationship type found for matching");
                }
            }
        }
        LOGGER.debug("Persisting...");
        repository.setElement(composedServiceTemplateId, composedServiceTemplate);
        LOGGER.debug("Persisted.");

        return composedServiceTemplateId;
    }

    /**
     *
     */
    public void resolveTopologyTemplate(ServiceTemplateId serviceTemplateId) throws SplittingException, IOException {
        IRepository repository = RepositoryFactory.getRepository();
        TServiceTemplate serviceTemplate = repository.getElement(serviceTemplateId);
        TTopologyTemplate topologyTemplate = serviceTemplate.getTopologyTemplate();

        List<TRequirement> openRequirements = getOpenRequirements(serviceTemplate);

        for (TRequirement requirement : openRequirements) {
            QName requiredCapTypeQName = getRequiredCapabilityTypeQNameOfRequirement(requirement);
            List<TNodeTemplate> nodesWithMatchingCapability = topologyTemplate.getNodeTemplates().stream()
                .filter(nt -> nt.getCapabilities() != null)
                .filter(nt -> nt.getCapabilities().stream()
                    .anyMatch(c -> c.getType().equals(requiredCapTypeQName)))
                .collect(Collectors.toList());

            if (!nodesWithMatchingCapability.isEmpty() && nodesWithMatchingCapability.size() == 1) {
                TCapability matchingCapability = nodesWithMatchingCapability.get(0).getCapabilities()
                    .stream().filter(c -> c.getType().equals(requiredCapTypeQName)).findFirst().get();
                TRelationshipType matchingRelationshipType =
                    getMatchingRelationshipType(requirement, matchingCapability);
                if (matchingRelationshipType != null) {
                    addMatchingRelationshipTemplateToTopologyTemplate(topologyTemplate, matchingRelationshipType, requirement, matchingCapability);
                } else {
                    throw new SplittingException("No suitable relationship type found for matching");
                }
            }
        }
        repository.setElement(serviceTemplateId, serviceTemplate);
    }

    /*
     * Checks if a topology template is valid.
     * The topology is valid if (1) all highest node templates have target labels assigned and
     * (2) all successor nodes connected by hostedOn relationships have no other target labels then the predecessors.
     *
     * @param topologyTemplate the topology template which should be checked
     * @return true if the topology template is valid and false if it is not
     */
    public boolean checkValidTopology(TServiceTemplate serviceTemplate) {
        Map<TNodeTemplate, Set<TNodeTemplate>> transitiveAndDirectSuccessors = computeTransitiveClosure(serviceTemplate.getTopologyTemplate());

        if (ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate) != null) {
            List<TNodeTemplate> appSpecificComponentsOfOtherParticipants = serviceTemplate.getTopologyTemplate().getNodeTemplates();
            appSpecificComponentsOfOtherParticipants.removeIf(nt -> ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate).equalsIgnoreCase(ModelUtilities.getParticipant(nt).get()));

            transitiveAndDirectSuccessors.remove(appSpecificComponentsOfOtherParticipants);
        }

        //check if the highest level node templates have target labels assigned
        for (TNodeTemplate node : getNodeTemplatesWithoutIncomingHostedOnRelationships(serviceTemplate)) {
            if (!ModelUtilities.getTargetLabel(node).isPresent()) {
                return false;
            }
        }

        //check the transitive closure for each node if nodes with different labels are reachable by hostedOn relationships
        for (TNodeTemplate node : transitiveAndDirectSuccessors.keySet()) {
            if (!transitiveAndDirectSuccessors.get(node).isEmpty()) {
                for (TNodeTemplate successor : transitiveAndDirectSuccessors.get(node)) {
                    if (ModelUtilities.getTargetLabel(successor).isPresent() && ModelUtilities.getTargetLabel(node).isPresent()
                        && !ModelUtilities.getTargetLabel(node).get().equalsIgnoreCase(ModelUtilities.getTargetLabel(successor).get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Splits a topology template according to the attached target labels. The target labels attached to nodes determine
     * at which target the nodes should be deployed. The result is a topology template containing for each target the
     * required nodes. Duplicates nodes which host nodes with different target labels.
     *
     * @param serviceTemplate the topology template which should be split
     * @return split topologyTemplate
     */
    public TServiceTemplate split(TServiceTemplate serviceTemplate) throws SplittingException {
        if (!checkValidTopology(serviceTemplate)) {
            throw new SplittingException("Topology is not valid");
        }

        // Copy for incremental removal of the processed nodes
        TServiceTemplate serviceTemplateCopy = BackendUtils.clone(serviceTemplate);
        serviceTemplateCopy.setTags(serviceTemplate.getTags());

        HashSet<TNodeTemplate> nodeTemplatesWhichPredecessorsHasNoPredecessors
            = new HashSet<>(getNodeTemplatesWhichPredecessorsHasNoPredecessors(serviceTemplateCopy));

        // Consider each node which hostedOn-predecessor nodes have no further hostedOn-predecessors
        while (!nodeTemplatesWhichPredecessorsHasNoPredecessors.isEmpty()) {

            for (TNodeTemplate currentNode : nodeTemplatesWhichPredecessorsHasNoPredecessors) {

                List<TNodeTemplate> predecessors = getHostedOnPredecessorsOfNodeTemplate(serviceTemplateCopy.getTopologyTemplate(), currentNode);
                Set<String> predecessorsTargetLabel = new HashSet<>();

                for (TNodeTemplate predecessor : predecessors) {
                    Optional<String> targetLabel = ModelUtilities.getTargetLabel(predecessor);
                    if (!targetLabel.isPresent()) {
                        LOGGER.error("No target label present");
                        LOGGER.error("id " + predecessor.getId());
                    }

                    //noinspection OptionalGetWithoutIsPresent
                    predecessorsTargetLabel.add(targetLabel.get().toLowerCase());
                }
                // If all predecessors have the same target label assign this label to the considered node
                if (predecessorsTargetLabel.size() == 1) {
                    //noinspection OptionalGetWithoutIsPresent
                    ModelUtilities.setTargetLabel(currentNode, ModelUtilities.getTargetLabel(predecessors.get(0)).get());
                } else {

                    List<TRelationshipTemplate> incomingRelationships
                        = ModelUtilities.getIncomingRelationshipTemplates(serviceTemplateCopy.getTopologyTemplate(), currentNode);
                    List<TRelationshipTemplate> outgoingRelationships
                        = ModelUtilities.getOutgoingRelationshipTemplates(serviceTemplateCopy.getTopologyTemplate(), currentNode);

                    // Otherwise, duplicate the considered node for each target label
                    for (String targetLabel : predecessorsTargetLabel) {
                        TNodeTemplate duplicatedNode = BackendUtils.clone(currentNode);
                        duplicatedNode.setId(Util.makeNCName(currentNode.getId() + "-" + targetLabel));
                        duplicatedNode.setName(Util.makeNCName(currentNode.getName()));
                        // rename capabilities and requirements
                        if (Objects.nonNull(duplicatedNode.getCapabilities())) {
                            duplicatedNode.getCapabilities().forEach(capability -> 
                                capability.setId(capability.getId() + "-" + targetLabel + "_" + idCounter++)
                            );
                        }
                        if (listIsNotNullOrEmpty(duplicatedNode.getRequirements())) {
                            duplicatedNode.getRequirements().forEach(requirement -> 
                                requirement.setId(requirement.getId() + "-" + targetLabel + "_" + idCounter++)
                            );
                        }

                        serviceTemplate.getTopologyTemplate().addNodeTemplate(duplicatedNode);
                        serviceTemplateCopy.getTopologyTemplate().addNodeTemplate(duplicatedNode);
                        ModelUtilities.setTargetLabel(duplicatedNode, targetLabel);

                        for (TRelationshipTemplate incomingRelationship : incomingRelationships) {
                            /*
                             * incoming hostedOn relationships from predecessors with the same label and not hostedOn
                             * relationships (e.g. conntectsTo) are assigned to the duplicated node.
                             * The origin relationships are duplicated
                             */
                            TNodeTemplate sourceNodeTemplate = ModelUtilities.getSourceNodeTemplateOfRelationshipTemplate(serviceTemplateCopy.getTopologyTemplate(), incomingRelationship);
                            if (((ModelUtilities.getTargetLabel(sourceNodeTemplate).get()
                                .equalsIgnoreCase(ModelUtilities.getTargetLabel(duplicatedNode).get())
                                && getBaseRelationshipType(incomingRelationship.getType()).getQName().equals(ToscaBaseTypes.hostedOnRelationshipType))
                                || !predecessors.contains(sourceNodeTemplate))) {

                                List<TRelationshipTemplate> reassignRelationship = new ArrayList<>();
                                reassignRelationship.add(incomingRelationship);
                                //Reassign incoming relationships
                                List<TRelationshipTemplate> reassignedRelationship
                                    = reassignIncomingRelationships(reassignRelationship, duplicatedNode);
                                serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().addAll(reassignedRelationship);
                                serviceTemplateCopy.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().addAll(reassignedRelationship);
                            }
                        }

                        /*
                         * Reassign outgoing relationships. No difference between the relationship types.
                         * Origin outgoing relationships are duplicated and added to the duplicated node as source
                         */
                        List<TRelationshipTemplate> newOutgoingRelationships
                            = reassignOutgoingRelationships(outgoingRelationships, duplicatedNode);
                        serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().addAll(newOutgoingRelationships);
                        serviceTemplateCopy.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().addAll(newOutgoingRelationships);
                    }

                    // Remove the original node and its relations from the origin topology template and the copy
                    serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().remove(currentNode);
                    serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(outgoingRelationships);
                    serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(incomingRelationships);
                    serviceTemplateCopy.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().remove(currentNode);
                    serviceTemplateCopy.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(outgoingRelationships);
                    serviceTemplateCopy.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(incomingRelationships);
                }

                // Remove the hostedOn-predecessors of the considered node and their relations in the working copy
                serviceTemplateCopy.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(predecessors);
                List<TRelationshipTemplate> removingRelationships =
                    serviceTemplateCopy.getTopologyTemplate().getRelationshipTemplates().stream()
                        .filter(rt -> predecessors.contains(ModelUtilities.getSourceNodeTemplateOfRelationshipTemplate(serviceTemplateCopy.getTopologyTemplate(), rt))
                            || predecessors.contains(ModelUtilities.getTargetNodeTemplateOfRelationshipTemplate(serviceTemplateCopy.getTopologyTemplate(), rt)))
                        .collect(Collectors.toList());

                serviceTemplateCopy.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(removingRelationships);
            }
            nodeTemplatesWhichPredecessorsHasNoPredecessors.clear();
            nodeTemplatesWhichPredecessorsHasNoPredecessors.addAll(getNodeTemplatesWhichPredecessorsHasNoPredecessors(serviceTemplateCopy));
        }

        return serviceTemplate;
    }

    /**
     * This method returns the possible hosts for each lowest level node. Before a suitable node is found nodes may be
     * removed.
     *
     * @param serviceTemplate which should be mapped to the cloud provider according to the attached target labels -
     *                        target labels have to be attached to each node
     * @return map with a list of possible hosts for each lowest level node
     */
    public Map<String, List<TServiceTemplate>> getHostingInjectionOptions(TServiceTemplate serviceTemplate) throws
        SplittingException {
        ProviderRepository repository = new ProviderRepository();
        Map<String, List<TServiceTemplate>> injectionOptions = new HashMap<>();
        // Contains all nodes for which at least one cloud provider node is found to host them
        List<TNodeTemplate> nodesForWhichHostsFound = new ArrayList<>();
        nodesForWhichHostsFound.clear();

        List<TNodeTemplate> needHostNodeTemplateCandidates = getNodeTemplatesWithoutOutgoingHostedOnRelationships(serviceTemplate);
        List<TNodeTemplate> nodesToCheck = new ArrayList<>();

        //Find lowest level nodes with open requirements which means they can be hosted by an other component
        for (TNodeTemplate nodeTemplateCandidate : needHostNodeTemplateCandidates) {
            if (hasNodeOpenRequirement(serviceTemplate, nodeTemplateCandidate)) {
                if (nodeTemplateCandidate.getRequirements() != null && nodeTemplateCandidate.getRequirements().stream()
                    .anyMatch(req -> getBasisCapabilityType(getRequiredCapabilityTypeQNameOfRequirement(req)).getName().equalsIgnoreCase("Container"))) {
                    nodesToCheck.add(nodeTemplateCandidate);
                }
            }
        }

        LOGGER.debug("Start...");
        if (!nodesToCheck.isEmpty()) {
            //Check all lowest level nodes with open requirements if a compatible node is available
            for (TNodeTemplate needHostNode : nodesToCheck) {
                Optional<String> label = ModelUtilities.getTargetLabel(needHostNode);
                if (!label.isPresent()) {
                    LOGGER.error("No target label present");
                    LOGGER.error("id " + needHostNode.getId());
                    throw new SplittingException("No target label present for Node Template " + needHostNode.getId());
                }

                //noinspection OptionalGetWithoutIsPresent
                String targetLabel = ModelUtilities.getTargetLabel(needHostNode).get();

                List<TRequirement> openHostedOnRequirements = needHostNode.getRequirements().stream()
                    .filter(req -> getBasisCapabilityType(getRequiredCapabilityTypeQNameOfRequirement(req)).getName().equalsIgnoreCase("Container")).collect(Collectors.toList());

                List<TServiceTemplate> compatibleTopologyFragments = repository
                    .getAllTopologyFragmentsForLocationAndOfferingCapability(targetLabel, openHostedOnRequirements.get(0));

                LOGGER.debug("Found {} compatible topology fragments for NodeTemplate {}",
                    compatibleTopologyFragments.size(), needHostNode.getId());

                //Add compatible nodes to the injectionOptions to host the considered lowest level node
                if (!compatibleTopologyFragments.isEmpty()) {
                    injectionOptions.put(needHostNode.getId(), compatibleTopologyFragments);
                    nodesForWhichHostsFound.add(needHostNode);
                }
            }
        }

        LOGGER.debug("Nodes to check: {}; Nodes with found host: {}", nodesToCheck.size(),
            nodesForWhichHostsFound.size());

        /*
         * Only the lowest components which are not in the matching list and which have still hostedOn-predecessors
         * are candidates which can be replaced by an other node
         */
        List<TNodeTemplate> replacementNodeTemplateCandidates = getReplacementNodeTemplateCandidatesForMatching(serviceTemplate, nodesForWhichHostsFound);

        while (!replacementNodeTemplateCandidates.isEmpty()) {
            for (TNodeTemplate replacementCandidate : replacementNodeTemplateCandidates) {
                List<TNodeTemplate> predecessorsOfReplacementCandidate = getHostedOnPredecessorsOfNodeTemplate(serviceTemplate.getTopologyTemplate(), replacementCandidate);
                Optional<String> label = ModelUtilities.getTargetLabel(replacementCandidate);
                if (!label.isPresent()) {
                    LOGGER.error("No target label present");
                    LOGGER.error("id " + replacementCandidate.getId());
                    throw new SplittingException("No target label present for Node Template " + replacementCandidate.getId());
                }

                //noinspection OptionalGetWithoutIsPresent
                String targetLabel = ModelUtilities.getTargetLabel(replacementCandidate).get();

                // For each replacement candidate the predecessors are considered
                for (TNodeTemplate predecessor : predecessorsOfReplacementCandidate) {
                    // Check if a compatible node for the predecessor from the right provider is available
                    if (predecessor.getRequirements() == null) {
                        nodesForWhichHostsFound.add(predecessor);
                        //throw new SplittingException("The Node Template with the ID " + predecessor.getId() + " has no requirement assigned and the injected can't be processed");
                    } else {
                        List<TRequirement> openHostedOnRequirements = predecessor.getRequirements().stream()
                            .filter(req -> getBasisCapabilityType(getRequiredCapabilityTypeQNameOfRequirement(req)).getName().equalsIgnoreCase("Container")).collect(Collectors.toList());

                        List<TServiceTemplate> compatibleTopologyFragments = repository
                            .getAllTopologyFragmentsForLocationAndOfferingCapability(targetLabel, openHostedOnRequirements.get(0));
                        //Add compatible nodes to the injectionOptions to host the considered lowest level node
                        if (!compatibleTopologyFragments.isEmpty()) {
                            injectionOptions.put(predecessor.getId(), compatibleTopologyFragments);
                            nodesForWhichHostsFound.add(predecessor);
                        }
                    }
                }
            }
            // Delete all replacement candidates and their relationships.
            serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(replacementNodeTemplateCandidates);
            List<TRelationshipTemplate> removingIncomingRelationships = ModelUtilities.getAllRelationshipTemplates(serviceTemplate.getTopologyTemplate())
                .stream()
                .filter(ir -> replacementNodeTemplateCandidates.contains(ir.getTargetElement().getRef()))
                .collect(Collectors.toList());

            serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(removingIncomingRelationships);
            List<TRelationshipTemplate> removingOutgoingRelationships = ModelUtilities.getAllRelationshipTemplates(serviceTemplate.getTopologyTemplate())
                .stream()
                .filter(ir -> replacementNodeTemplateCandidates.contains(ir.getSourceElement().getRef()))
                .collect(Collectors.toList());

            serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(removingIncomingRelationships);
            serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate().removeAll(removingOutgoingRelationships);
            //The former predecessors now are either matched by a new host or they are new replacement candidates
            replacementNodeTemplateCandidates.clear();
            replacementNodeTemplateCandidates.addAll(getReplacementNodeTemplateCandidatesForMatching(serviceTemplate, nodesForWhichHostsFound));
        }

        /*
         * Check if all lowest level nodes are contained in the nodesForWhichHostsFound, i.e., at least one
         * cloud provider node is found to host them.
         * The application-specific nodes must not be replacement candidates!
         */
        List<TNodeTemplate> checkListAllNodesMatched = serviceTemplate.getTopologyTemplate().getNodeTemplates().stream()
            .filter(z -> getNodeTemplatesWithoutOutgoingHostedOnRelationships(serviceTemplate).contains(z))
            .filter(node -> hasNodeOpenRequirement(serviceTemplate, node))
            .filter(y -> !nodesForWhichHostsFound.contains(y))
            .collect(Collectors.toList());

        LOGGER.debug("{} nodes without matching:", checkListAllNodesMatched.size());
        for (TNodeTemplate node : checkListAllNodesMatched) {
            LOGGER.debug(node.getId());
        }
        if (!checkListAllNodesMatched.isEmpty()) {
            throw new SplittingException("No matching possible");
        }
        return injectionOptions;
    }

    /**
     * Check if the given NodeTemplate has an open requirement.
     *
     * @param serviceTemplate the topology containing the NodeTemplate
     * @param node            the NodeTemplate to check
     * @return <code>true</code> if an open requirement is found, <code>false</code> otherwise
     */
    private boolean hasNodeOpenRequirement(TServiceTemplate serviceTemplate, TNodeTemplate node) {
        if (Objects.isNull(node.getRequirements())) {
            return false;
        }
        List<TRequirement> openRequirements = getOpenRequirements(serviceTemplate);
        return node.getRequirements().stream().anyMatch(openRequirements::contains);
    }

    /**
     * @param serviceTemplate which has to be labeled with the default label '*' which includes all available provider
     *                        repositories and then it is searched for matching options
     * @return map with the host options for each lowest node of the topology
     */
    public Map<String, List<TServiceTemplate>> getHostingMatchingOptionsWithDefaultLabeling(TServiceTemplate
                                                                                                serviceTemplate) throws SplittingException {
        if (!checkApplicationSpecificComponentTargetLabeling(serviceTemplate)) {
            serviceTemplate.getTopologyTemplate().getNodeTemplates().forEach(t -> ModelUtilities.setTargetLabel(t, "*"));
        } else if (checkValidTopology(serviceTemplate)) {
            Map<TNodeTemplate, Set<TNodeTemplate>> transitiveAndDirectSuccessors = computeTransitiveClosure(serviceTemplate.getTopologyTemplate());
            List<TNodeTemplate> appSpecificComponents = getNodeTemplatesWithoutIncomingHostedOnRelationships(serviceTemplate);

            List<TNodeTemplate> NodesOfOtherParticipants = serviceTemplate.getTopologyTemplate().getNodeTemplates();
            NodesOfOtherParticipants.removeIf(nt -> ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate).equalsIgnoreCase(ModelUtilities.getParticipant(nt).get()));

            transitiveAndDirectSuccessors.remove(NodesOfOtherParticipants);
            //target label must be set for all hostedOn- Successors of app-specific Components
            for (TNodeTemplate appSpecificComponent : appSpecificComponents) {
                for (TNodeTemplate successor : transitiveAndDirectSuccessors.get(appSpecificComponent)) {
                    if (!ModelUtilities.getTargetLabel(successor).isPresent() && ModelUtilities.getTargetLabel(appSpecificComponent).isPresent()) {
                        ModelUtilities.setTargetLabel(successor, ModelUtilities.getTargetLabel(appSpecificComponent).get());
                    }
                }
            }
        } else {
            throw new SplittingException("Topology is not valid");
        }
        return getHostingInjectionOptions(serviceTemplate);
    }

    private boolean checkApplicationSpecificComponentTargetLabeling(TServiceTemplate serviceTemplate) {

        List<TNodeTemplate> nodeTemplates = getNodeTemplatesWithoutIncomingHostedOnRelationships(serviceTemplate);
        if (ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate) != null) {
            nodeTemplates = nodeTemplates.stream().filter(nt -> ModelUtilities.getParticipant(nt).isPresent())
                .filter(nt -> ModelUtilities.getParticipant(nt).get().equalsIgnoreCase(ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate)))
                .collect(Collectors.toList());
        }

        return nodeTemplates.stream().allMatch(nt -> ModelUtilities.getTargetLabel(nt).isPresent());
    }

    /**
     * Selects default the first entry of the list of possible hosts for each node, which requires a new host
     *
     * @param serviceTemplate which need new hosts added
     * @return topologyTemplate with randomly chosen hosts
     */
    public TTopologyTemplate hostMatchingWithDefaultHostSelection(TServiceTemplate serviceTemplate) throws
        SplittingException {
        TServiceTemplate newServiceTemplate = BackendUtils.clone(serviceTemplate);
        Map<String, List<TServiceTemplate>> matchingOptions = getHostingInjectionOptions(newServiceTemplate);
        Map<String, TTopologyTemplate> defaultHostSelection = new HashMap<>();
        matchingOptions.entrySet().forEach(entry -> defaultHostSelection.put(entry.getKey(), entry.getValue().get(0).getTopologyTemplate()));

        return injectNodeTemplates(serviceTemplate.getTopologyTemplate(), defaultHostSelection, InjectRemoval.REMOVE_REPLACED_AND_SUCCESSORS);
    }

    public TTopologyTemplate connectionMatchingWithDefaultConnectorSelection(TServiceTemplate serviceTemplate) throws
        SplittingException {
        Map<String, List<TServiceTemplate>> connectionInjectionOptions = getConnectionInjectionOptions(serviceTemplate);
        Map<String, TTopologyTemplate> defaultConnectorSelection = new HashMap<>();
        connectionInjectionOptions.entrySet().forEach(entry -> defaultConnectorSelection.put(entry.getKey(), entry.getValue().get(0).getTopologyTemplate()));

        return injectConnectionNodeTemplates(serviceTemplate.getTopologyTemplate(), defaultConnectorSelection);
    }

    public String calculateChoreographyTag(List<TNodeTemplate> nodeTemplateList, String participantName) {
        String choreoValue = "";
        // iterate over node templates and check if their target location == current participant
        for (TNodeTemplate tNodeTemplate : nodeTemplateList) {
            Optional<String> nodeOwner = ModelUtilities.getParticipant(tNodeTemplate);
            if (nodeOwner.isPresent() && nodeOwner.get().equalsIgnoreCase(participantName)) {
                choreoValue += tNodeTemplate.getId() + ",";
            }
        }

        choreoValue = choreoValue.substring(0, choreoValue.length() - 1);

        return choreoValue;
    }

    /**
     * Replaces the host of each key by the value of the map. Adds new relationships between the nodes and their new
     * hosts
     *
     * @param topologyTemplate original topology for which the Node Templates shall be replaced
     * @param injectNodes      map with the Nodes to replace as key and the replacement as value
     * @param removal          remove nothing, only replaced nt or replaced nt and all successors
     * @return modified topology with the replaced Node Templates
     */
    public TTopologyTemplate injectNodeTemplates(TTopologyTemplate
                                                     topologyTemplate, Map<String, TTopologyTemplate> injectNodes, InjectRemoval removal) throws
        SplittingException {
        String id;

        // Matching contains all cloud provider nodes matched to the topology
        List<TNodeTemplate> matching = new ArrayList<>();
        matching.clear();
        LOGGER.debug("Start Matching Method");

        Set<TNodeTemplate> replacedNodeTemplatesToDelete = new HashSet<>();

        for (String predecessorOfNewHostId : injectNodes.keySet()) {
            TNodeTemplate predecessorOfNewHost = topologyTemplate.getNodeTemplates().stream()
                .filter(nt -> nt.getId().equals(predecessorOfNewHostId))
                .findFirst()
                .orElse(null);
            LOGGER.debug("Predecessor which get a new host " + predecessorOfNewHost.getId());

            List<TNodeTemplate> originHostSuccessors = new ArrayList<>();
            originHostSuccessors.clear();
            originHostSuccessors = getHostedOnSuccessorsOfNodeTemplate(topologyTemplate, predecessorOfNewHost);
            TRequirement openHostedOnRequirement = predecessorOfNewHost.getRequirements().stream()
                .filter(req -> getBasisCapabilityType(getRequiredCapabilityTypeQNameOfRequirement(req)).getName().equals("Container"))
                .findAny().get();
            TNodeTemplate newMatchingNodeTemplate;
            TTopologyTemplate matchingTopologyFragment = injectNodes.get(predecessorOfNewHostId);

            //Highest Node Template to which the HostedOn Relationship has to be connected
            TNodeTemplate newHostNodeTemplate = matchingTopologyFragment.getNodeTemplates().stream()
                .filter(nt -> nt.getCapabilities() != null)
                .filter(nt -> nt.getCapabilities().stream().anyMatch(cap -> cap.getType().equals(getRequiredCapabilityTypeQNameOfRequirement(openHostedOnRequirement))))
                .findFirst().get();
            LOGGER.debug("New host NodeTemplate: {}", newHostNodeTemplate.getId());

            //Check if the chosen replace node is already in the matching
            if (matching.stream()
                .anyMatch(nt -> equalsWithDifferentId(nt, newHostNodeTemplate))) {
                newMatchingNodeTemplate = matching.stream()
                    .filter(nt -> equalsWithDifferentId(nt, newHostNodeTemplate)).findAny().get();
            } else {
                newMatchingNodeTemplate = newHostNodeTemplate;
                matchingTopologyFragment.getNodeTemplateOrRelationshipTemplate().stream()
                    .filter(et -> topologyTemplate.getNodeTemplateOrRelationshipTemplate().stream().anyMatch(tet -> tet.getId().equals(et.getId())))
                    .forEach(et -> et.setId(et.getId() + "_" + idCounter++));

                // rename capabilities and requirements
                matchingTopologyFragment.getNodeTemplates().forEach(node -> {
                    List<TCapability> caps = node.getCapabilities();
                    if (Objects.nonNull(caps)) {
                        caps.stream()
                            .filter(et -> topologyTemplate.getNodeTemplates().stream()
                                .filter(nt -> Objects.nonNull(nt.getCapabilities()))
                                .flatMap(nt -> nt.getCapabilities().stream())
                                .anyMatch(cap -> cap.getId().equals(et.getId())))
                            .forEach(et -> et.setId(et.getId() + "_" + idCounter++));
                    }

                    if (listIsNotNullOrEmpty(node.getRequirements())) {
                        node.getRequirements().stream()
                            .filter(et -> topologyTemplate.getNodeTemplates().stream()
                                .filter(nt -> Objects.nonNull(nt.getRequirements()))
                                .flatMap(nt -> nt.getRequirements().stream())
                                .anyMatch(req -> req.getId().equals(et.getId()))
                            ).forEach(et -> et.setId(et.getId() + "_" + idCounter++));
                    }
                });

                LOGGER.debug("Add {} NodeTemplate(s)",
                    matchingTopologyFragment.getNodeTemplateOrRelationshipTemplate().size());
                topologyTemplate.getNodeTemplateOrRelationshipTemplate().addAll(matchingTopologyFragment.getNodeTemplateOrRelationshipTemplate());
                matching.add(newMatchingNodeTemplate);
            }

            //In case the predecessor was a lowest node a new hostedOn relationship has to be added
            if (originHostSuccessors.isEmpty()) {
                TRelationshipTemplate newHostedOnRelationship = new TRelationshipTemplate();
                List<String> ids = new ArrayList<>();
                List<TRelationshipTemplate> tRelationshipTemplates = ModelUtilities.getAllRelationshipTemplates(topologyTemplate);
                tRelationshipTemplates.forEach(rt -> ids.add(rt.getId()));
                //Check if counter is already set in another Id, if yes -> increase newRelationshipCounter +1
                boolean uniqueID = false;
                id = "0";
                while (!uniqueID) {
                    if (!ids.contains("con" + newRelationshipIdCounter)) {
                        id = "con_" + newRelationshipIdCounter;
                        newRelationshipIdCounter++;
                        uniqueID = true;
                    } else {
                        newRelationshipIdCounter++;
                    }
                }
                newHostedOnRelationship.setId(id);
                newHostedOnRelationship.setName(id);

                TRelationshipTemplate.SourceOrTargetElement sourceElement = new TRelationshipTemplate.SourceOrTargetElement();
                TRelationshipTemplate.SourceOrTargetElement targetElement = new TRelationshipTemplate.SourceOrTargetElement();
                sourceElement.setRef(predecessorOfNewHost);
                targetElement.setRef(newMatchingNodeTemplate);
                newHostedOnRelationship.setSourceElement(sourceElement);
                newHostedOnRelationship.setTargetElement(targetElement);

                TRequirement requiredRequirement = null;
                List<TRequirement> openRequirements = predecessorOfNewHost.getRequirements();
                for (TRequirement requirement : openRequirements) {
                    QName requiredCapabilityTypeQName = getRequiredCapabilityTypeQNameOfRequirement(requirement);
                    TCapabilityType matchingBasisCapabilityType = getBasisCapabilityType(requiredCapabilityTypeQName);

                    if (matchingBasisCapabilityType.getName().equalsIgnoreCase("Container")) {
                        requiredRequirement = requirement;
                    }
                }

                TCapability requiredCapability = null;
                List<TCapability> openCapabilities = newMatchingNodeTemplate.getCapabilities();
                for (TCapability capability : openCapabilities) {
                    TCapabilityType basisCapabilityType = getBasisCapabilityType(capability.getType());
                    if (basisCapabilityType.getName().equalsIgnoreCase("Container")) {
                        requiredCapability = capability;
                    }
                }
                if (requiredRequirement == null || requiredCapability == null) {
                    throw new SplittingException("The predecessor or new host node has matching which requires a hostedOn relationship");
                } else {
                    TRelationshipType relationshipType = getMatchingRelationshipType(requiredRequirement, requiredCapability);
                    if (relationshipType != null) {
                        QName relationshipTypeQName = new QName(relationshipType.getTargetNamespace(), relationshipType.getName());
                        newHostedOnRelationship.setType(relationshipTypeQName);
                    } else {
                        throw new SplittingException("No suitable relationship type available for hosting the node template with the ID "
                            + predecessorOfNewHost.getId());
                    }
                }

                topologyTemplate.getNodeTemplateOrRelationshipTemplate().add(newHostedOnRelationship);
            } else {
                LOGGER.debug("Predecessor has successor NodeTemplates...");
                //Assupmtion: Only one hostedOn Successor possible
                TNodeTemplate originHost = originHostSuccessors.get(0);
                List<TRelationshipTemplate> incomingRelationshipsOfReplacementCandidate =
                    ModelUtilities.getIncomingRelationshipTemplates(topologyTemplate, originHost);

                //The incoming Relationships not from the predecessors have to be copied
                List<TRelationshipTemplate> incomingRelationshipsNotHostedOn =
                    incomingRelationshipsOfReplacementCandidate.stream()
                        .filter(r -> !getHostedOnPredecessorsOfNodeTemplate(topologyTemplate, originHost)
                            .contains(ModelUtilities.getSourceNodeTemplateOfRelationshipTemplate(topologyTemplate, r)))
                        .collect(Collectors.toList());

                topologyTemplate.getNodeTemplateOrRelationshipTemplate()
                    .addAll(reassignIncomingRelationships(incomingRelationshipsNotHostedOn, newMatchingNodeTemplate));

                //The incoming Relationships from the currently considered predecessor should be switched
                incomingRelationshipsOfReplacementCandidate.stream()
                    .filter(rt -> rt.getSourceElement().getRef().equals(predecessorOfNewHost))
                    .forEach(rt -> rt.getTargetElement().setRef(newMatchingNodeTemplate));

                // All outgoing rel of the origin hosts are copied and reassigned to the new host as source
                List<TRelationshipTemplate> outgoingRelationshipsOfReplacementCandidateNotHostedOn =
                    ModelUtilities.getOutgoingRelationshipTemplates(topologyTemplate, originHost)
                        .stream()
                        .filter(r -> !getHostedOnSuccessorsOfNodeTemplate(topologyTemplate, originHost).contains(r.getTargetElement().getRef()))
                        .collect(Collectors.toList());

                topologyTemplate.getNodeTemplateOrRelationshipTemplate()
                    .addAll(reassignOutgoingRelationships(outgoingRelationshipsOfReplacementCandidateNotHostedOn, newMatchingNodeTemplate));

                replacedNodeTemplatesToDelete.add(originHost);
            }
        }

        switch (removal) {
            case REMOVE_NOTHING:
                return topologyTemplate;
            case REMOVE_REPLACED:
                for (TNodeTemplate deleteOriginNode : replacedNodeTemplatesToDelete) {
                    topologyTemplate.getNodeTemplateOrRelationshipTemplate()
                        .removeAll(ModelUtilities
                            .getIncomingRelationshipTemplates(topologyTemplate, deleteOriginNode));
                    topologyTemplate.getNodeTemplateOrRelationshipTemplate()
                        .removeAll(ModelUtilities
                            .getOutgoingRelationshipTemplates(topologyTemplate, deleteOriginNode));
                }
                topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(replacedNodeTemplatesToDelete);
                return topologyTemplate;
            default:
                break;
        }

        Map<TNodeTemplate, Set<TNodeTemplate>> transitiveAndDirectSuccessors = computeTransitiveClosure(topologyTemplate);
        // Delete all replaced Nodes and their direct and transitive hostedOn successors
        for (TNodeTemplate deleteOriginNode : replacedNodeTemplatesToDelete) {
            if (!transitiveAndDirectSuccessors.get(deleteOriginNode).isEmpty()) {
                for (TNodeTemplate successor : transitiveAndDirectSuccessors.get(deleteOriginNode)) {
                    topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(ModelUtilities.getIncomingRelationshipTemplates(topologyTemplate, successor));
                    topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(ModelUtilities.getOutgoingRelationshipTemplates(topologyTemplate, successor));
                }
            }
            topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(transitiveAndDirectSuccessors.get(deleteOriginNode));
            topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(ModelUtilities.getIncomingRelationshipTemplates(topologyTemplate, deleteOriginNode));
            topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(ModelUtilities.getOutgoingRelationshipTemplates(topologyTemplate, deleteOriginNode));
        }
        topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(replacedNodeTemplatesToDelete);

        LOGGER.debug("Resulting topology has {} NodeTemplates...", topologyTemplate.getNodeTemplates().size());
        return topologyTemplate;
    }

    /**
     *
     */
    public Map<String, List<TServiceTemplate>> getConnectionInjectionOptions(TServiceTemplate serviceTemplate) throws
        SplittingException {
        ProviderRepository providerRepository = new ProviderRepository();
        Map<String, List<TServiceTemplate>> connectionInjectionOptions = new HashMap<>();
        List<TNodeTemplate> nodeTemplates = ModelUtilities.getAllNodeTemplates(serviceTemplate.getTopologyTemplate());
        List<TNodeTemplate> nodeTemplatesWithConnectionRequirement = nodeTemplates.stream()
            .filter(nt -> nt.getRequirements() != null)
            .filter(nt -> nt.getRequirements().stream()
                .anyMatch(req -> getBasisCapabilityType(getRequiredCapabilityTypeQNameOfRequirement(req)).getName().equalsIgnoreCase("Endpoint")))
            .collect(Collectors.toList());

        if (!nodeTemplatesWithConnectionRequirement.isEmpty()) {
            for (TNodeTemplate nodeWithOpenConnectionRequirement : nodeTemplatesWithConnectionRequirement) {
                List<TRequirement> requirements = nodeWithOpenConnectionRequirement.getRequirements().stream()
                    .filter(req -> getBasisCapabilityType(getRequiredCapabilityTypeQNameOfRequirement(req)).getName().equalsIgnoreCase("Endpoint"))
                    .filter(req -> getOpenRequirementsAndMatchingBasisCapabilityTypeNames(serviceTemplate).keySet().contains(req))
                    .collect(Collectors.toList());

                for (TRequirement openRequirement : requirements) {

                    List<TServiceTemplate> matchingTopologyFragments = providerRepository.getAllTopologyFragmentsForLocationAndOfferingCapability("*",
                        openRequirement);

                    if (!matchingTopologyFragments.isEmpty()) {
                        // instead of the Node Template ID the Requirement ID is added because one Node Template can occur multiple times for connection injection
                        connectionInjectionOptions.put(openRequirement.getId(), matchingTopologyFragments);
                    } else {
                        throw new SplittingException("No matching found");
                    }
                }
            }
        } else {
            connectionInjectionOptions = null;
            LOGGER.debug("No open requirements found");
        }
        return connectionInjectionOptions;
    }

    /**
     *
     */

    public TTopologyTemplate injectConnectionNodeTemplates(TTopologyTemplate
                                                               topologyTemplate, Map<String, TTopologyTemplate> selectedConnectionFragments)
        throws SplittingException {
        List<TNodeTemplate> nodeTemplates = ModelUtilities.getAllNodeTemplates(topologyTemplate);
        for (String openRequirementId : selectedConnectionFragments.keySet()) {
            TNodeTemplate nodeTemplateWithThisOpenReq = nodeTemplates.stream()
                .filter(nt -> nt.getRequirements() != null)
                .filter(nt -> nt.getRequirements().stream().anyMatch(req -> req.getId().equals(openRequirementId)))
                .findFirst().get();

            TRequirement openRequirement = nodeTemplateWithThisOpenReq.getRequirements().stream()
                .filter(req -> req.getId().equals(openRequirementId)).findFirst().get();

            QName requiredCapabilityTypeQName = getRequiredCapabilityTypeQNameOfRequirement(openRequirement);

            selectedConnectionFragments.get(openRequirementId).getNodeTemplateOrRelationshipTemplate().stream()
                .filter(et -> topologyTemplate.getNodeTemplateOrRelationshipTemplate().stream().anyMatch(tet -> tet.getId().equals(et.getId())))
                .forEach(et -> et.setId(et.getId() + "_" + idCounter++));

            topologyTemplate.getNodeTemplateOrRelationshipTemplate()
                .addAll(selectedConnectionFragments.get(openRequirementId).getNodeTemplateOrRelationshipTemplate());
            nodeTemplates.addAll(ModelUtilities.getAllNodeTemplates(selectedConnectionFragments.get(openRequirementId)));

            TNodeTemplate nodeWithOpenCapability = nodeTemplates.stream()
                .filter(nt -> nt.getCapabilities() != null)
                .filter(nt -> nt.getCapabilities().stream()
                    .anyMatch(c -> c.getType().equals(requiredCapabilityTypeQName))).findFirst().get();
            TCapability matchingCapability = nodeWithOpenCapability.getCapabilities()
                .stream().filter(c -> c.getType().equals(requiredCapabilityTypeQName)).findFirst().get();

            TRelationshipType matchingRelationshipType =
                getMatchingRelationshipType(openRequirement, matchingCapability);

            if (matchingRelationshipType != null) {

                addMatchingRelationshipTemplateToTopologyTemplate(topologyTemplate, matchingRelationshipType, openRequirement, matchingCapability);
            } else {
                throw new SplittingException("No suitable relationship type found for matching");
            }
        }
        return topologyTemplate;
    }

    /**
     *
     */
    public Map<TRequirement, TNodeTemplate> getOpenRequirementsAndItsNodeTemplate(TTopologyTemplate
                                                                                      topologyTemplate) {
        Map<TRequirement, TNodeTemplate> openRequirementsAndItsNodeTemplates = new HashMap<>();
        List<TNodeTemplate> nodeTemplates = topologyTemplate.getNodeTemplates();

        for (TNodeTemplate nodeTemplate : nodeTemplates) {
            if (nodeTemplate.getRequirements() != null) {
                extractSuccessorsOfNode(topologyTemplate, nodeTemplates, nodeTemplate);
                List<TNodeTemplate> successorsOfNodeTemplate = extractSuccessorsOfNode(topologyTemplate, nodeTemplates, nodeTemplate);
                for (TRequirement requirement : nodeTemplate.getRequirements()) {
                    QName requiredCapabilityTypeQName = getRequiredCapabilityTypeQNameOfRequirement(requirement);

                    if (!successorsOfNodeTemplate.isEmpty()) {
                        boolean existingCap = successorsOfNodeTemplate.stream()
                            .filter(x -> x.getCapabilities() != null)
                            .anyMatch(x -> x.getCapabilities().stream().anyMatch(y -> y.getType().equals(requiredCapabilityTypeQName)));
                        if (!existingCap) {
                            openRequirementsAndItsNodeTemplates.put(requirement, nodeTemplate);
                        }
                    } else {
                        openRequirementsAndItsNodeTemplates.put(requirement, nodeTemplate);
                    }
                }
            }
        }
        return openRequirementsAndItsNodeTemplates;
    }

    private List<TNodeTemplate> extractSuccessorsOfNode(TTopologyTemplate
                                                            topologyTemplate, List<TNodeTemplate> nodeTemplates, TNodeTemplate nodeTemplate) {
        List<TNodeTemplate> successorsOfNodeTemplate = new ArrayList<>();
        List<TRelationshipTemplate> outgoingRelationships = ModelUtilities.getOutgoingRelationshipTemplates(topologyTemplate, nodeTemplate);
        if (listIsNotNullOrEmpty(outgoingRelationships)) {
            for (TRelationshipTemplate relationshipTemplate : outgoingRelationships) {
                if (relationshipTemplate.getSourceElement().getRef() instanceof TNodeTemplate) {
                    successorsOfNodeTemplate.add((TNodeTemplate) relationshipTemplate.getTargetElement().getRef());
                } else {
                    TCapability targetElement = (TCapability) relationshipTemplate.getTargetElement().getRef();
                    successorsOfNodeTemplate.add(nodeTemplates.stream()
                        .filter(nt -> nt.getCapabilities() != null)
                        .filter(nt -> nt.getCapabilities().stream().anyMatch(c -> c.getId().equals(targetElement.getId()))).findAny().get());
                }
            }
        }

        return successorsOfNodeTemplate;
    }

    /**
     *
     */
    private TRelationshipType getMatchingRelationshipType(TRequirement requirement, TCapability capability) {

        TRelationshipType matchingRelationshipType = null;

        SortedSet<RelationshipTypeId> relTypeIds = RepositoryFactory.getRepository().getAllDefinitionsChildIds(RelationshipTypeId.class);
        List<TRelationshipType> relationshipTypes = new ArrayList<>();
        for (RelationshipTypeId id : relTypeIds) {
            relationshipTypes.add(RepositoryFactory.getRepository().getElement(id));
        }

        Map<String, String> requirementProperties = ModelUtilities.getPropertiesKV(requirement);
        Map<String, String> capabilityProperties = ModelUtilities.getPropertiesKV(capability);

        /* If the property "requiredRelationshipType" is defined for the requirement and the capability this relationship type
           has to be taken - if the specified relationship type is not available, no relationship type is chosen */
        if (requirementProperties != null && capabilityProperties != null &&
            requirementProperties.containsKey("requiredRelationshipType") && capabilityProperties.containsKey("requiredRelationshipType")
            && requirementProperties.get("requiredRelationshipType").equals(capabilityProperties.get("requiredRelationshipType"))
            && requirementProperties.get("requiredRelationshipType") != null) {
            // Assumption: We work on basic KV properties here
            QName referencedRelationshipType = QName.valueOf((String) requirementProperties.get("requiredRelationshipType"));
            RelationshipTypeId relTypeId = new RelationshipTypeId(referencedRelationshipType);
            if (relTypeIds.stream().anyMatch(rti -> rti.equals(relTypeId))) {
                return RepositoryFactory.getRepository().getElement(relTypeId);
            }
        } else {
            QName requirementTypeQName = requirement.getType();
            RequirementTypeId reqTypeId = new RequirementTypeId(requirement.getType());
            TRequirementType requirementType = RepositoryFactory.getRepository().getElement(reqTypeId);

            QName capabilityTypeQName = capability.getType();
            CapabilityTypeId capTypeId = new CapabilityTypeId(capability.getType());
            TCapabilityType capabilityType = RepositoryFactory.getRepository().getElement(capTypeId);

            List<TRelationshipType> availableMatchingRelationshipTypes = new ArrayList<>();
            availableMatchingRelationshipTypes.clear();

            while (requirementType != null && capabilityType != null) {

                //relationship type with valid source origin requirement type or empty and valid target origin capability type
                for (TRelationshipType rt : relationshipTypes) {
                    if ((rt.getValidSource() == null || rt.getValidSource().getTypeRef().equals(requirementTypeQName)) && (rt.getValidTarget() != null && rt.getValidTarget().getTypeRef().equals(capabilityTypeQName))) {
                        availableMatchingRelationshipTypes.add(rt);
                    }
                }

                if (!availableMatchingRelationshipTypes.isEmpty() && availableMatchingRelationshipTypes.size() == 1) {
                    return availableMatchingRelationshipTypes.get(0);
                } else if (!availableMatchingRelationshipTypes.isEmpty() && availableMatchingRelationshipTypes.size() > 1) {
                    return null;
                } else if (requirementType.getDerivedFrom() != null || capabilityType.getDerivedFrom() != null) {

                    TCapabilityType derivedFromCapabilityType = null;
                    TRequirementType derivedFromRequirementType = null;

                    availableMatchingRelationshipTypes.clear();
                    List<TRelationshipType> additionalMatchingRelationshipTypes = new ArrayList<>();

                    if (capabilityType.getDerivedFrom() != null) {
                        QName derivedFromCapabilityTypeRef = capabilityType.getDerivedFrom().getTypeRef();
                        CapabilityTypeId derivedFromCapTypeId = new CapabilityTypeId(derivedFromCapabilityTypeRef);
                        derivedFromCapabilityType = RepositoryFactory.getRepository().getElement(derivedFromCapTypeId);

                        for (TRelationshipType rt : relationshipTypes) {
                            if ((rt.getValidSource() == null || rt.getValidSource().getTypeRef().equals(requirementTypeQName)) && (rt.getValidTarget() != null && rt.getValidTarget().getTypeRef().equals(derivedFromCapabilityTypeRef))) {
                                availableMatchingRelationshipTypes.add(rt);
                            }
                        }
                    }
                    if (requirementType.getDerivedFrom() != null) {
                        QName derivedFromRequirementTypeRef = requirementType.getDerivedFrom().getTypeRef();
                        RequirementTypeId derivedFromReqTypeId = new RequirementTypeId(derivedFromRequirementTypeRef);
                        derivedFromRequirementType = RepositoryFactory.getRepository().getElement(derivedFromReqTypeId);

                        for (TRelationshipType rt : relationshipTypes) {
                            if ((rt.getValidSource() != null && rt.getValidSource().getTypeRef().equals(derivedFromRequirementTypeRef)) && (rt.getValidTarget() != null && rt.getValidTarget().getTypeRef().equals(capabilityTypeQName))) {
                                additionalMatchingRelationshipTypes.add(rt);
                            }
                        }
                    }

                    availableMatchingRelationshipTypes.addAll(additionalMatchingRelationshipTypes);

                    if (!availableMatchingRelationshipTypes.isEmpty() && availableMatchingRelationshipTypes.size() == 1) {
                        return availableMatchingRelationshipTypes.get(0);
                    } else if (!availableMatchingRelationshipTypes.isEmpty() && availableMatchingRelationshipTypes.size() > 1) {
                        return null;
                    }

                    requirementType = derivedFromRequirementType;
                    capabilityType = derivedFromCapabilityType;
                }
            }

            TCapabilityType basisCapabilityType = getBasisCapabilityType(capability.getType());

            for (TRelationshipType relationshipType : relationshipTypes) {
                if (basisCapabilityType != null && basisCapabilityType.getName().equalsIgnoreCase("container")
                    && relationshipType.getName().equalsIgnoreCase("hostedon") && relationshipType.getValidSource() == null
                    && (relationshipType.getValidTarget() == null || relationshipType.getValidTarget().getTypeRef().getLocalPart().equalsIgnoreCase("container"))) {
                    return relationshipType;
                }
                if (basisCapabilityType != null && basisCapabilityType.getName().equalsIgnoreCase("endpoint")
                    && relationshipType.getName().equalsIgnoreCase("connectsto") && relationshipType.getValidSource() == null
                    && (relationshipType.getValidTarget() == null || relationshipType.getValidTarget().getTypeRef().getLocalPart().equalsIgnoreCase("endpoint"))) {
                    return relationshipType;
                }
            }
        }
        return matchingRelationshipType;
    }

    /**
     * Switch the source of a relationship to a new node template
     *
     * @param outgoingRel all relationships which has to be switched to a new source element
     * @param newSource   the new source element
     * @return list of the reassigned relationships
     */
    private List<TRelationshipTemplate> reassignOutgoingRelationships
    (List<TRelationshipTemplate> outgoingRel, TNodeTemplate newSource) {

        List<TRelationshipTemplate> newOutgoingRel = new ArrayList<>();
        for (TRelationshipTemplate outgoingRelationship : outgoingRel) {
            TRelationshipTemplate newOutgoingRelationship = BackendUtils.clone(outgoingRelationship);
            TRelationshipTemplate.SourceOrTargetElement sourceElementNew = new TRelationshipTemplate.SourceOrTargetElement();
            sourceElementNew.setRef(newSource);
            newOutgoingRelationship.setSourceElement(sourceElementNew);

            //TargetLable is only appended if not done yet
            if (outgoingRelationship.getId().contains(ModelUtilities.getTargetLabel(newSource).get())) {
                newOutgoingRelationship.setId(Util.makeNCName(outgoingRelationship.getId()));
                newOutgoingRelationship.setName(Util.makeNCName(outgoingRelationship.getName()));
            } else {
                newOutgoingRelationship.setId(Util.makeNCName(outgoingRelationship.getId() + "-" + ModelUtilities.getTargetLabel(newSource).get()));
                newOutgoingRelationship.setName(Util.makeNCName(outgoingRelationship.getName() + "-" + ModelUtilities.getTargetLabel(newSource).get()));
            }
            newOutgoingRel.add(newOutgoingRelationship);
        }
        return newOutgoingRel;
    }

    /**
     * Switch the target of a relationship to a new node template
     *
     * @param incomingRel all relationships which has to be switched to a new target element
     * @param newTarget   the new target element
     * @return list of the reassigned relationships
     */
    private List<TRelationshipTemplate> reassignIncomingRelationships
    (List<TRelationshipTemplate> incomingRel, TNodeTemplate newTarget) {
        List<TRelationshipTemplate> newIncomingRel = new ArrayList<>();
        for (TRelationshipTemplate incomingRelationship : incomingRel) {
            TRelationshipTemplate newIncomingRelationship = BackendUtils.clone(incomingRelationship);
            TRelationshipTemplate.SourceOrTargetElement targetElementNew = new TRelationshipTemplate.SourceOrTargetElement();
            targetElementNew.setRef(newTarget);
            newIncomingRelationship.setTargetElement(targetElementNew);

            newIncomingRelationship.setId(Util.makeNCName(incomingRelationship.getId()));
            newIncomingRelationship.setName(Util.makeNCName(incomingRelationship.getName()));

            newIncomingRel.add(newIncomingRelationship);
        }
        return newIncomingRel;
    }

    /**
     * Find all lowest level nodes which are not application-specific nodes which are not contained in the
     * matchingNodeTemplates list
     *
     * @param serviceTemplate       topology template
     * @param matchingNodeTemplates lowest level nodes contained in this list are not considered as candidates
     * @return list of all node templates which are candidates for replacement
     */
    protected List<TNodeTemplate> getReplacementNodeTemplateCandidatesForMatching(TServiceTemplate
                                                                                      serviceTemplate, List<TNodeTemplate> matchingNodeTemplates) {

        return ModelUtilities.getAllNodeTemplates(serviceTemplate.getTopologyTemplate())
            .stream()
            .filter(y -> !matchingNodeTemplates.contains(y))
            .filter(y -> !getNodeTemplatesWithoutIncomingHostedOnRelationships(serviceTemplate).contains(y))
            .filter(z -> getNodeTemplatesWithoutOutgoingHostedOnRelationships(serviceTemplate).contains(z))
            .collect(Collectors.toList());
    }

    /**
     * Find all node templates which has no incoming hostedOn relationships (highest level nodes)
     *
     * @return list of node templates
     */
    protected List<TNodeTemplate> getNodeTemplatesWithoutIncomingHostedOnRelationships(TServiceTemplate
                                                                                           serviceTemplate) {

        List<TNodeTemplate> nodeTemplates = serviceTemplate.getTopologyTemplate().getNodeTemplates()
            .stream()
            .filter(nt -> getHostedOnPredecessorsOfNodeTemplate(serviceTemplate.getTopologyTemplate(), nt).isEmpty())
            .collect(Collectors.toList());

        if (ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate) != null) {
            return nodeTemplates.stream().filter(nt -> ModelUtilities.getParticipant(nt).isPresent())
                .filter(nt -> ModelUtilities.getParticipant(nt).get().equalsIgnoreCase(ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate)))
                .collect(Collectors.toList());
        }
        return nodeTemplates;
    }

    /**
     * Find all node templates which has no outgoing hostedOn relationships (lowest level nodes)
     *
     * @return list of node templates
     */
    protected List<TNodeTemplate> getNodeTemplatesWithoutOutgoingHostedOnRelationships(TServiceTemplate
                                                                                           serviceTemplate) {
        List<TNodeTemplate> participantNodes = new ArrayList<>();
        List<TNodeTemplate> nodeTemplates = serviceTemplate.getTopologyTemplate().getNodeTemplates().stream()
            .filter(nt -> getHostedOnSuccessorsOfNodeTemplate(serviceTemplate.getTopologyTemplate(), nt).isEmpty())
            .collect(Collectors.toList());
        if (ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate) != null) {

            participantNodes = nodeTemplates.stream().filter(nt -> ModelUtilities.getParticipant(nt).isPresent())
                .filter(nt -> ModelUtilities.getParticipant(nt).get().equalsIgnoreCase(ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate)))
                .collect(Collectors.toList());
            return participantNodes;
        }
        return nodeTemplates;
    }

    /**
     * Find all node templates which predecessors has no further predecessors
     *
     * @return list of nodes
     */
    protected List<TNodeTemplate> getNodeTemplatesWhichPredecessorsHasNoPredecessors(TServiceTemplate
                                                                                         serviceTemplate) {
        List<TNodeTemplate> nodeTemplates = new ArrayList<>();
        if (ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate) != null) {
            nodeTemplates = serviceTemplate.getTopologyTemplate().getNodeTemplates().stream().filter(nt -> ModelUtilities.getParticipant(nt).isPresent())
                .filter(nt -> ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate).equalsIgnoreCase(ModelUtilities.getParticipant(nt).get()))
                .collect(Collectors.toList());
        }

        List<TNodeTemplate> predecessorsOfpredecessors = new ArrayList<>();
        predecessorsOfpredecessors.clear();
        List<TNodeTemplate> candidates = new ArrayList<>();
        for (TNodeTemplate nodeTemplate : nodeTemplates) {
            List<TNodeTemplate> allPredecessors = getHostedOnPredecessorsOfNodeTemplate(serviceTemplate.getTopologyTemplate(), nodeTemplate);
            if (!allPredecessors.isEmpty()) {
                predecessorsOfpredecessors.clear();
                for (TNodeTemplate predecessor : allPredecessors) {
                    predecessorsOfpredecessors.addAll(getHostedOnPredecessorsOfNodeTemplate(serviceTemplate.getTopologyTemplate(), predecessor));
                }
                if (predecessorsOfpredecessors.isEmpty()) {
                    candidates.add(nodeTemplate);
                }
            }
        }
        return candidates;
    }

    /**
     * Find all successors of a node template. the successor is the source of a hostedOn relationship to the
     * nodeTemplate
     *
     * @param nodeTemplate for which all successors should be found
     * @return list of successors (node templates)
     */
    protected static List<TNodeTemplate> getHostedOnSuccessorsOfNodeTemplate(TTopologyTemplate
                                                                                 topologyTemplate, TNodeTemplate nodeTemplate) {
        List<TNodeTemplate> successorNodeTemplates = new ArrayList<>();
        for (TRelationshipTemplate relationshipTemplate : ModelUtilities.getOutgoingRelationshipTemplates(topologyTemplate, nodeTemplate)) {
            if (getBaseRelationshipType(relationshipTemplate.getType()).getQName().equals(ToscaBaseTypes.hostedOnRelationshipType)) {
                successorNodeTemplates.add(ModelUtilities.getTargetNodeTemplateOfRelationshipTemplate(topologyTemplate, relationshipTemplate));
            }
        }
        return successorNodeTemplates;
    }

    /**
     * Find all predecessors of a node template. the predecessor is the target of a hostedOn relationship to the
     * nodeTemplate
     *
     * @param nodeTemplate for which all predecessors should be found
     * @return list of predecessors
     */
    protected static List<TNodeTemplate> getHostedOnPredecessorsOfNodeTemplate(TTopologyTemplate
                                                                                   topologyTemplate, TNodeTemplate nodeTemplate) {
        List<TNodeTemplate> predecessorNodeTemplates = new ArrayList<>();
        predecessorNodeTemplates.clear();
        List<TRelationshipTemplate> incomingRelationships = ModelUtilities.getIncomingRelationshipTemplates(topologyTemplate, nodeTemplate);
        for (TRelationshipTemplate relationshipTemplate : incomingRelationships) {
            if (getBaseRelationshipType(relationshipTemplate.getType()).getQName().equals(ToscaBaseTypes.hostedOnRelationshipType)) {
                predecessorNodeTemplates.add(ModelUtilities.getSourceNodeTemplateOfRelationshipTemplate(topologyTemplate, relationshipTemplate));
            }
        }
        return predecessorNodeTemplates;
    }

    /**
     * Compute transitive closure of a given topology template based on the hostedOn relationships
     */

    public Map<TNodeTemplate, Set<TNodeTemplate>> computeTransitiveClosure(TTopologyTemplate topologyTemplate) {
        List<TNodeTemplate> nodeTemplates = new ArrayList<>(topologyTemplate.getNodeTemplates());

        for (TNodeTemplate node : nodeTemplates) {
            initDirectSuccessors.put(node, new HashSet<>(getHostedOnSuccessorsOfNodeTemplate(topologyTemplate, node)));
            visitedNodeTemplates.put(node, false);
            transitiveAndDirectSuccessors.put(node, new HashSet<>());
        }
        for (TNodeTemplate node : nodeTemplates) {
            if (!visitedNodeTemplates.get(node)) {
                computeNodeForTransitiveClosure(node);
            }
        }

        return transitiveAndDirectSuccessors;
    }

    /**
     * Helper method to compute the transitive closure
     */
    private void computeNodeForTransitiveClosure(TNodeTemplate nodeTemplate) {
        visitedNodeTemplates.put(nodeTemplate, true);
        Set<TNodeTemplate> successorsToCheck;
        successorsToCheck = initDirectSuccessors.get(nodeTemplate);
        successorsToCheck.removeAll(transitiveAndDirectSuccessors.get(nodeTemplate));

        for (TNodeTemplate successorToCheck : successorsToCheck) {
            if (!visitedNodeTemplates.get(successorToCheck)) {
                computeNodeForTransitiveClosure(successorToCheck);
            }
            transitiveAndDirectSuccessors.get(nodeTemplate).add(successorToCheck);
            transitiveAndDirectSuccessors.get(nodeTemplate).addAll(transitiveAndDirectSuccessors.get(successorToCheck));
        }
    }

    public Map<TRequirement, String> getOpenRequirementsAndMatchingBasisCapabilityTypeNames(TServiceTemplate
                                                                                                serviceTemplate) {
        Map<TRequirement, String> openRequirementsAndMatchingBaseCapability = new HashMap<>();
        List<TRequirement> openRequirements = getOpenRequirements(serviceTemplate);
        if (openRequirements != null && !openRequirements.isEmpty()) {
            for (TRequirement requirement : openRequirements) {
                QName requiredCapabilityTypeQName = getRequiredCapabilityTypeQNameOfRequirement(requirement);
                TCapabilityType matchingBasisCapabilityType = getBasisCapabilityType(requiredCapabilityTypeQName);
                openRequirementsAndMatchingBaseCapability.put(requirement, matchingBasisCapabilityType.getName());
            }
            return openRequirementsAndMatchingBaseCapability;
        } else return null;
    }

    /**
     *
     */
    public List<TRequirement> getOpenRequirements(TServiceTemplate serviceTemplate) {
        List<TRequirement> openRequirements = new ArrayList<>();
        List<TNodeTemplate> nodeTemplates = serviceTemplate.getTopologyTemplate().getNodeTemplates();

        if (ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate) != null) {
            nodeTemplates.stream().filter(nt -> ModelUtilities.getParticipant(nt).isPresent())
                .filter(nt -> ModelUtilities.getParticipant(nt).get().equalsIgnoreCase(ModelUtilities.getOwnerParticipantOfServiceTemplate(serviceTemplate)))
                .collect(Collectors.toList());
        }

        for (TNodeTemplate nodeTemplate : nodeTemplates) {
            if (nodeTemplate.getRequirements() != null) {
                List<TNodeTemplate> successorsOfNodeTemplate =
                    extractSuccessorsOfNode(serviceTemplate.getTopologyTemplate(), nodeTemplates, nodeTemplate);
                for (TRequirement requirement : nodeTemplate.getRequirements()) {
                    QName requiredCapabilityTypeQName = getRequiredCapabilityTypeQNameOfRequirement(requirement);

                    if (!successorsOfNodeTemplate.isEmpty()) {
                        boolean existingCap = successorsOfNodeTemplate.stream()
                            .filter(x -> x.getCapabilities() != null)
                            .anyMatch(x -> x.getCapabilities().stream().anyMatch(y -> y.getType().equals(requiredCapabilityTypeQName)));
                        if (!existingCap) {
                            openRequirements.add(requirement);
                        }
                    } else {
                        openRequirements.add(requirement);
                    }
                }
            }
        }
        return openRequirements;
    }

    private TCapabilityType getBasisCapabilityType(QName capabilityTypeQName) {
        CapabilityTypeId parentCapTypeId = new CapabilityTypeId(capabilityTypeQName);
        TCapabilityType parentCapabilityType = RepositoryFactory.getRepository().getElement(parentCapTypeId);
        TCapabilityType basisCapabilityType = parentCapabilityType;

        while (parentCapabilityType != null) {
            basisCapabilityType = parentCapabilityType;

            if (parentCapabilityType.getDerivedFrom() != null) {
                capabilityTypeQName = parentCapabilityType.getDerivedFrom().getTypeRef();
                parentCapTypeId = new CapabilityTypeId(capabilityTypeQName);
                parentCapabilityType = RepositoryFactory.getRepository().getElement(parentCapTypeId);
            } else {
                parentCapabilityType = null;
            }
        }

        return basisCapabilityType;
    }

    public QName getRequiredCapabilityTypeQNameOfRequirement(TRequirement requirement) {
        QName reqTypeQName = requirement.getType();
        RequirementTypeId reqTypeId = new RequirementTypeId(reqTypeQName);
        TRequirementType requirementType = RepositoryFactory.getRepository().getElement(reqTypeId);
        return requirementType.getRequiredCapabilityType();
    }

    public static TRelationshipType getBaseRelationshipType(QName relationshipTypeQName) {
        RelationshipTypeId parentRelationshipTypeId = new RelationshipTypeId(relationshipTypeQName);
        TRelationshipType parentRelationshipType = RepositoryFactory.getRepository().getElement(parentRelationshipTypeId);
        TRelationshipType basisRelationshipType = parentRelationshipType;

        while (parentRelationshipType != null) {
            basisRelationshipType = parentRelationshipType;

            if (parentRelationshipType.getDerivedFrom() != null) {
                relationshipTypeQName = parentRelationshipType.getDerivedFrom().getTypeRef();
                parentRelationshipTypeId = new RelationshipTypeId(relationshipTypeQName);
                parentRelationshipType = RepositoryFactory.getRepository().getElement(parentRelationshipTypeId);
            } else {
                parentRelationshipType = null;
            }
        }
        return basisRelationshipType;
    }

    private void addMatchingRelationshipTemplateToTopologyTemplate(TTopologyTemplate
                                                                       topologyTemplate, TRelationshipType relationshipType, TRequirement requirement, TCapability capability) {

        TRelationshipTemplate matchingRelationshipTemplate = new TRelationshipTemplate();

        QName relationshipTypeQName = new QName(relationshipType.getTargetNamespace(), relationshipType.getName());
        LOGGER.debug("The QName of the matchingRelationshipType for ReqCap Matching", relationshipTypeQName);
        List<String> ids = new ArrayList<>();
        List<TRelationshipTemplate> tRelationshipTemplates = ModelUtilities.getAllRelationshipTemplates(topologyTemplate);
        tRelationshipTemplates.forEach(rt -> ids.add(rt.getId()));
        //Check if counter is already set in another Id, if yes -> increase newRelationshipCounter +1
        boolean uniqueID = false;
        String id = "0";
        while (!uniqueID) {
            if (!ids.contains("con" + newRelationshipIdCounter)) {
                id = "con_" + newRelationshipIdCounter;
                newRelationshipIdCounter++;
                uniqueID = true;
            } else {
                newRelationshipIdCounter++;
            }
        }
        matchingRelationshipTemplate.setId(id);
        matchingRelationshipTemplate.setName(id);
        matchingRelationshipTemplate.setType(relationshipTypeQName);
        TRelationshipTemplate.SourceOrTargetElement sourceElement = new TRelationshipTemplate.SourceOrTargetElement();
        TRelationshipTemplate.SourceOrTargetElement targetElement = new TRelationshipTemplate.SourceOrTargetElement();
        sourceElement.setRef(requirement);
        targetElement.setRef(capability);
        matchingRelationshipTemplate.setSourceElement(sourceElement);
        matchingRelationshipTemplate.setTargetElement(targetElement);
        topologyTemplate.getNodeTemplateOrRelationshipTemplate().add(matchingRelationshipTemplate);
    }

    /**
     * Check if the two given NodeTemplates are considered equal in terms of matching, which means that this
     * NodeTemplate is only injected once and used as a hostedOn predecessor for multiple other NodeTemplates. The
     * equality exists if the two NodeTemplates have the same target label and are equal with regard to all attributes
     * except the Id as the Id is replaced during matching.
     *
     * @param node1 the first NodeTemplate to compare
     * @param node2 the second NodeTemplate to compare
     * @return <code>true</code> if the two NodeTemplates are considered equal, <code>false</code> otherwise
     */
    private boolean equalsWithDifferentId(TNodeTemplate node1, TNodeTemplate node2) {
        if (node1 == node2) return true;

        // check if the two NodeTemplates have the same target label defined
        if (!node1.getOtherAttributes().get(ModelUtilities.QNAME_LOCATION)
            .equalsIgnoreCase(node2.getOtherAttributes().get(ModelUtilities.QNAME_LOCATION))) {
            return false;
        }

        // check properties if they are defined (just equals on properties does not seem to work)
        if (Objects.nonNull(node1.getProperties()) && Objects.nonNull(node2.getProperties())
            && Objects.nonNull(ModelUtilities.getPropertiesKV(node1))
            && Objects.nonNull(ModelUtilities.getPropertiesKV(node2))) {
            LinkedHashMap<String, String> properties1 = ModelUtilities.getPropertiesKV(node1);
            LinkedHashMap<String, String> properties2 = ModelUtilities.getPropertiesKV(node2);
            if (!properties1.equals(properties2)) {
                return false;
            }
        }
        if (Objects.nonNull(node1.getRequirements()) && Objects.nonNull(node2.getRequirements())
            && Objects.nonNull(node1.getCapabilities())
            && Objects.nonNull(node2.getCapabilities())) {
            List<String> reqTypeNamesNode1 = new ArrayList<>();
            node1.getRequirements().stream().forEach(req -> reqTypeNamesNode1.add(req.getType().toString()));
            List<String> reqTypeNamesNode2 = new ArrayList<>();
            node2.getRequirements().stream().forEach(req -> reqTypeNamesNode2.add(req.getType().toString()));

            List<String> capTypeNamesNode1 = new ArrayList<>();
            node1.getCapabilities().stream().forEach(cap -> capTypeNamesNode1.add(cap.getType().toString()));
            List<String> capTypeNamesNode2 = new ArrayList<>();
            node2.getCapabilities().stream().forEach(cap -> capTypeNamesNode2.add(cap.getType().toString()));
            if (!capTypeNamesNode1.equals(capTypeNamesNode2) || !reqTypeNamesNode1.equals(reqTypeNamesNode2)) {
                return false;
            }
        }

        if (Objects.nonNull(node1.getPolicies()) && Objects.nonNull(node2.getPolicies())) {
            List<String> policyTypesNode1 = new ArrayList<>();
            node1.getPolicies().stream().forEach(policy -> policyTypesNode1.add(policy.getPolicyType().toString()));
            List<String> policyTypesNode2 = new ArrayList<>();
            node2.getPolicies().stream().forEach(policy -> policyTypesNode2.add(policy.getPolicyType().toString()));
            if (!policyTypesNode1.equals(policyTypesNode2)) {
                return false;
            }
        }

        // check if the NodeTemplates are equal (except Id which is replaced while matching)
        return Objects.equals(node1.getPropertyConstraints(), node2.getPropertyConstraints()) &&
            Objects.equals(node1.getType(), node2.getType()) &&
            Objects.equals(node1.getDeploymentArtifacts(), node2.getDeploymentArtifacts()) &&
            Objects.equals(node1.getName(), node2.getName()) &&
            Objects.equals(node1.getMinInstances(), node2.getMinInstances()) &&
            Objects.equals(node1.getMaxInstances(), node2.getMaxInstances());
    }
}
