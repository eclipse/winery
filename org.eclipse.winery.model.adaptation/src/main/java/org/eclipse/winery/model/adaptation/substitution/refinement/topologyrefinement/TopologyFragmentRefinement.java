/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.adaptation.substitution.refinement.topologyrefinement;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.configuration.DARefinementConfigurationObject;
import org.eclipse.winery.common.configuration.Environments;
import org.eclipse.winery.model.adaptation.substitution.refinement.AbstractRefinement;
import org.eclipse.winery.model.adaptation.substitution.refinement.DefaultRefinementChooser;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils;
import org.eclipse.winery.model.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.model.ids.definitions.ArtifactTypeId;
import org.eclipse.winery.model.ids.extensions.RefinementId;
import org.eclipse.winery.model.ids.extensions.TopologyFragmentRefinementModelId;
import org.eclipse.winery.model.tosca.HasId;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMapping;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMappingType;
import org.eclipse.winery.model.tosca.extensions.OTDeploymentArtifactMapping;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTRelationDirection;
import org.eclipse.winery.model.tosca.extensions.OTRelationMapping;
import org.eclipse.winery.model.tosca.extensions.OTStayMapping;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.model.version.VersionSupport;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.common.RepositoryFileReference;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateFilesDirectoryId;
import org.eclipse.winery.repository.exceptions.WineryRepositoryException;
import org.eclipse.winery.topologygraph.matching.IToscaMatcher;
import org.eclipse.winery.topologygraph.matching.ToscaPrmPropertyMatcher;
import org.eclipse.winery.topologygraph.model.ToscaEdge;
import org.eclipse.winery.topologygraph.model.ToscaGraph;
import org.eclipse.winery.topologygraph.model.ToscaNode;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jgrapht.GraphMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.redirectRelation;

public class TopologyFragmentRefinement extends AbstractRefinement {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologyFragmentRefinement.class);
    protected final Map<QName, TArtifactType> artifactTypes = new HashMap<>();

    public TopologyFragmentRefinement(RefinementChooser refinementChooser, Class<? extends RefinementId> idClass, String versionAppendix) {
        super(refinementChooser, idClass, versionAppendix);
        this.artifactTypes.putAll(this.repository.getQNameToElementMapping(ArtifactTypeId.class));
    }

    public TopologyFragmentRefinement(RefinementChooser refinementChooser) {
        this(refinementChooser, TopologyFragmentRefinementModelId.class, "refined");
    }

    @SuppressWarnings("unused")
    public TopologyFragmentRefinement() {
        this(new DefaultRefinementChooser());
    }

    @Override
    public boolean isApplicable(RefinementCandidate candidate, TTopologyTemplate topology) {
        return candidate.getDetectorGraph().vertexSet()
            .stream()
            .allMatch(vertex -> {
                TNodeTemplate matchingNode = candidate.getGraphMapping().getVertexCorrespondence(vertex, false).getTemplate();
                return this.canRedirectExternalRelations(candidate, vertex.getTemplate(), matchingNode, topology)
                    && this.canMoveDeploymentArtifacts(candidate, vertex.getTemplate(), matchingNode, topology);
            });
    }

    @Override
    public boolean getLoopCondition(TTopologyTemplate topology) {
        return true;
    }

    @Override
    public IToscaMatcher getMatcher(OTRefinementModel prm) {
        return new ToscaPrmPropertyMatcher(repository.getNamespaceManager());
    }

    @Override
    public Map<String, String> applyRefinement(RefinementCandidate refinement, TTopologyTemplate topology) {
        if (!(refinement.getRefinementModel() instanceof OTTopologyFragmentRefinementModel)) {
            throw new UnsupportedOperationException("The refinement candidate is not a PRM!");
        }

        // determine the elements that are staying
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();
        List<String> stayingRefinementElementIds = RefinementUtils.getStayingRefinementElements(prm).stream()
            .map(HasId::getId)
            .collect(Collectors.toList());

        // import the refinement structure
        Map<String, String> idMapping = BackendUtils.mergeTopologyTemplateAinTopologyTemplateB(
            refinement.getRefinementModel().getRefinementTopology(),
            topology,
            stayingRefinementElementIds
        );

        // only for UI: position the imported nodes next to the nodes to be refined
        Map<String, Map<String, Integer>> coordinates = calculateNewPositions(
            refinement.getDetectorGraph(),
            refinement.getGraphMapping(),
            refinement.getRefinementModel().getRefinementTopology()
        );
        refinement.getRefinementModel().getRefinementTopology().getNodeTemplates().stream()
            .filter(element -> !stayingRefinementElementIds.contains(element.getId()))
            .forEach(node -> {
                    Map<String, Integer> newCoordinates = coordinates.get(node.getId());
                    TNodeTemplate nodeTemplate = topology.getNodeTemplate(idMapping.get(node.getId()));
                    if (nodeTemplate != null) {
                        nodeTemplate.setX(newCoordinates.get("x").toString());
                        nodeTemplate.setY(newCoordinates.get("y").toString());
                    }
                }
            );

        // iterate over the detector nodes
        refinement.getDetectorGraph().vertexSet().forEach(vertex -> {
            // get the matching node in the topology
            TNodeTemplate matchingNode = refinement.getGraphMapping().getVertexCorrespondence(vertex, false).getTemplate();

            this.redirectInternalRelations(prm, vertex.getTemplate(), matchingNode, topology, idMapping);
            this.redirectExternalRelations(refinement, vertex.getTemplate(), matchingNode, topology, idMapping);

            this.applyPropertyMappings(refinement, vertex.getId(), matchingNode, topology, idMapping);
            this.applyDeploymentArtifactMapping(refinement, vertex.getTemplate(), matchingNode, topology, idMapping);

            if (getStayMappingsOfCurrentElement(prm, vertex.getTemplate()).findFirst().isEmpty()) {
                topology.getNodeTemplateOrRelationshipTemplate().remove(matchingNode);
            } else if (shouldRemoveBehaviorPatterns(vertex.getTemplate(), matchingNode)) {
                if (vertex.getTemplate().getPolicies() != null && matchingNode.getPolicies() != null)
                    vertex.getTemplate().getPolicies().forEach(detectorPolicy ->
                        matchingNode.getPolicies()
                            .removeIf(matchingPolicy ->
                                matchingPolicy.getPolicyType().equals(detectorPolicy.getPolicyType())
                            )
                    );
            }
        });
        refinement.getDetectorGraph().edgeSet()
            .forEach(edge -> {
                TRelationshipTemplate relationshipTemplate = refinement.getGraphMapping().getEdgeCorrespondence(edge, false).getTemplate();

                this.applyPropertyMappings(refinement, edge.getId(), relationshipTemplate, topology, idMapping);

                if (getStayMappingsOfCurrentElement(prm, edge.getTemplate()).findFirst().isEmpty()) {
                    topology.getNodeTemplateOrRelationshipTemplate().remove(relationshipTemplate);
                }
            });
        return idMapping;
    }

    protected boolean shouldRemoveBehaviorPatterns(TNodeTemplate vertex, TNodeTemplate matchingNode) {
        return vertex.getPolicies() != null
            && matchingNode.getPolicies() != null;
    }

    private boolean applyDeploymentArtifactMapping(RefinementCandidate refinement, TNodeTemplate detectorNode, TNodeTemplate matchingNode,
                                                   TTopologyTemplate topology, Map<String, String> idMapping) {
        List<OTDeploymentArtifactMapping> deploymentArtifactMappings = ((OTTopologyFragmentRefinementModel) refinement.getRefinementModel()).getDeploymentArtifactMappings();

        DARefinementConfigurationObject daServices = Environments.getInstance().getDaRefinementConfigurationObject();

        return matchingNode.getDeploymentArtifacts() == null
            || matchingNode.getDeploymentArtifacts().isEmpty()
            || (
            deploymentArtifactMappings != null && matchingNode.getDeploymentArtifacts().stream()
                .filter(deploymentArtifact -> deploymentArtifact.getArtifactType() != null)
                .allMatch(deploymentArtifact ->
                    deploymentArtifactMappings.stream()
                        .filter(mapping -> mapping.getDetectorElement().getId().equals(detectorNode.getId()))
                        .anyMatch(mapping -> {
                            if (ModelUtilities.isOfType(mapping.getArtifactType(), deploymentArtifact.getArtifactType(), this.artifactTypes)) {
                                // only do the actual transformation in the transformation step, not the "is-applicable" step
                                if (idMapping != null) {
                                    TNodeTemplate addedNode = topology.getNodeTemplate(idMapping.get(mapping.getRefinementElement().getId()));
                                    if (addedNode != null) {
                                        TDeploymentArtifact artifactToAdd = this.translateDeploymentArtifact(
                                            mapping, deploymentArtifact,
                                            Environments.getInstance().getDaRefinementConfigurationObject()
                                        );

                                        List<TDeploymentArtifact> existingDAs = addedNode.getDeploymentArtifacts();
                                        if (existingDAs == null) {
                                            existingDAs = new ArrayList<>();
                                            addedNode.setDeploymentArtifacts(existingDAs);
                                        } else if (
                                            existingDAs.stream()
                                                .anyMatch(da -> da.getName().equals(artifactToAdd.getName()))
                                        ) {
                                            artifactToAdd.setName(artifactToAdd.getName() + UUID.randomUUID());
                                        }
                                        existingDAs.add(artifactToAdd);
                                    } else {
                                        LOGGER.error("Error while adding Deployment Artifacts! Node was not added to the topology!");
                                    }
                                }
                                return true;
                            }
                            return false;
                        })
                )
        );
    }

    TDeploymentArtifact translateDeploymentArtifact(OTDeploymentArtifactMapping mapping, TDeploymentArtifact deploymentArtifact,
                                                    DARefinementConfigurationObject daTranslationServices) {
        if (daTranslationServices != null && daTranslationServices.getRefinementServices() != null) {
            Optional<Map.Entry<String, DARefinementConfigurationObject.DARefinementService>> applicableService =
                daTranslationServices.getRefinementServices().entrySet().stream()
                    .filter(entry -> {
                        DARefinementConfigurationObject.DARefinementService service = entry.getValue();

                        if (service.canRefine.from.contains(mapping.getArtifactType().getLocalPart())
                            && service.canRefine.to.contains(mapping.getTargetArtifactType().getLocalPart())) {
                            LOGGER.debug("Service '{}' can refine Artifact Type '{}' to '{}'!",
                                entry.getValue(), mapping.getArtifactType(), mapping.getTargetArtifactType());
                            return true;
                        }
                        return false;
                    }).findFirst();

            if (applicableService.isPresent()) {
                Map.Entry<String, DARefinementConfigurationObject.DARefinementService> serviceEntry = applicableService.get();

                QName artifact = deploymentArtifact.getArtifactRef();
                LOGGER.info("Using service '{}' to translate artifact '{}'", serviceEntry.getKey(), artifact);
                DARefinementConfigurationObject.DARefinementService service = serviceEntry.getValue();

                try (final CloseableHttpClient httpClient = HttpClients.createDefault();
                     final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                    ArtifactTemplateId artifactTemplateId = new ArtifactTemplateId(artifact);
                    ArtifactTemplateFilesDirectoryId filesDirectoryId = new ArtifactTemplateFilesDirectoryId(artifactTemplateId);
                    this.repository.getZippedContents(filesDirectoryId, byteArrayOutputStream);

                    try (final InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                        MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
                        multipartBuilder.addBinaryBody(
                            "file",
                            inputStream,
                            ContentType.APPLICATION_OCTET_STREAM,
                            "files.zip"
                        );

                        multipartBuilder.addTextBody("inputFormat", mapping.getArtifactType().getLocalPart());
                        multipartBuilder.addTextBody("outputFormat", mapping.getTargetArtifactType().getLocalPart());

                        HttpPost httpPost = new HttpPost(service.url);
                        httpPost.setEntity(multipartBuilder.build());
                        LOGGER.info("Sending request: {}", httpPost);

                        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode >= 200 && statusCode < 300) {
                                ArtifactTemplateId translatedArtifactId = new ArtifactTemplateId(
                                    artifactTemplateId.getNamespace().getDecoded(),
                                    VersionSupport.getNewComponentVersionId(artifactTemplateId, mapping.getTargetArtifactType().getLocalPart()),
                                    false
                                );
                                this.repository.setElement(
                                    translatedArtifactId,
                                    new TArtifactTemplate.Builder(translatedArtifactId.getXmlId().getDecoded(), mapping.getTargetArtifactType())
                                        .setName(translatedArtifactId.getXmlId().getDecoded())
                                        .build()
                                );
                                LOGGER.info("Created new Artifact Template with QName \"{}\"", translatedArtifactId.getQName());

                                String fileName = "translated";
                                if (response.getFirstHeader("Content-Disposition") != null) {
                                    String value = response.getFirstHeader("Content-Disposition").getValue();
                                    if (value != null) {
                                        for (String contentDisposition : value.split(" ")) {
                                            if (contentDisposition.startsWith("filename=") && contentDisposition.length() > "filename=".length() + 1) {
                                                fileName = contentDisposition.substring(contentDisposition.indexOf("=") + 1);
                                            }
                                        }
                                    }
                                }

                                LOGGER.info("Saving translated file \"{}\"", fileName);
                                ArtifactTemplateFilesDirectoryId filesId = new ArtifactTemplateFilesDirectoryId(translatedArtifactId);
                                if (response.getEntity() != null) {
                                    try (BufferedInputStream contentStream = new BufferedInputStream(response.getEntity().getContent())) {
                                        repository.putContentToFile(
                                            new RepositoryFileReference(filesId, fileName),
                                            contentStream
                                        );

                                        BackendUtils.synchronizeReferences(RepositoryFactory.getRepository(), translatedArtifactId);
                                        LOGGER.info("Successfully saved file!");

                                        TDeploymentArtifact translatedArtifact = new TDeploymentArtifact.Builder(deploymentArtifact.getName() + "-translated", mapping.getTargetArtifactType())
                                            .setArtifactRef(translatedArtifactId.getQName())
                                            .build();
                                        LOGGER.info("Created \"{}\"", translatedArtifact);

                                        return translatedArtifact;
                                    }
                                } else {
                                    LOGGER.error("Service did not respond with a translated file!");
                                }
                            } else {
                                LOGGER.error("Server responded with Status {} and error \"{}\"",
                                    statusCode,
                                    new String(response.getEntity().getContent().readAllBytes())
                                );
                            }
                        } catch (IOException e) {
                            LOGGER.error("Could not refine DA...!", e);
                            LOGGER.warn("Defaulting to already contained DA!");
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Could not refine DA...!", e);
                } catch (WineryRepositoryException e) {
                    LOGGER.error("Error while zipping the files in the artifact!", e);
                }
            }
        }

        LOGGER.warn("Defaulting to already contained DA!");

        return deploymentArtifact;
    }

    public void applyPropertyMappings(RefinementCandidate refinement, String detectorNodeId, TEntityTemplate matchingEntity,
                                      TTopologyTemplate topology, Map<String, String> idMapping) {
        List<OTAttributeMapping> propertyMappings = ((OTTopologyFragmentRefinementModel) refinement.getRefinementModel()).getAttributeMappings();
        if (Objects.nonNull(propertyMappings)) {
            propertyMappings.stream()
                .filter(mapping -> mapping.getDetectorElement().getId().equals(detectorNodeId))
                .forEach(mapping -> {
                    if (Objects.nonNull(matchingEntity.getProperties())) {
                        Map<String, String> sourceProperties = ModelUtilities.getPropertiesKV(matchingEntity);
                        topology.getNodeTemplateOrRelationshipTemplate()
                            .stream()
                            .filter(element -> element.getId().equals(idMapping.get(mapping.getRefinementElement().getId())))
                            .findFirst()
                            .ifPresent(addedElement -> {
                                if (addedElement.getProperties() != null) {
                                    LinkedHashMap<String, String> targetProperties = ModelUtilities.getPropertiesKV(addedElement);

                                    if (Objects.nonNull(sourceProperties) && !sourceProperties.isEmpty()
                                        && Objects.nonNull(targetProperties)) {
                                        if (mapping.getType() == OTAttributeMappingType.ALL) {
                                            sourceProperties.forEach(targetProperties::replace);
                                        } else {
                                            // TPrmPropertyMappingType.SELECTIVE
                                            String sourceValue = sourceProperties.get(mapping.getDetectorProperty());
                                            targetProperties.put(mapping.getRefinementProperty(), sourceValue);
                                        }
                                        // because of the dynamical generation of the KV properties, we must set them again to persist them...
                                        ModelUtilities.setPropertiesKV(addedElement, targetProperties);
                                    }
                                }
                            });
                    }
                });
        }
    }

    private boolean canRedirectExternalRelations(RefinementCandidate refinement, TNodeTemplate detectorNode, TNodeTemplate matchingNode, TTopologyTemplate topology) {
        return this.redirectExternalRelations(refinement, detectorNode, matchingNode, topology, null);
    }

    private boolean canMoveDeploymentArtifacts(RefinementCandidate candidate, TNodeTemplate detectorNode, TNodeTemplate matchingNode, TTopologyTemplate topology) {
        return this.applyDeploymentArtifactMapping(candidate, detectorNode, matchingNode, topology, null);
    }

    private boolean redirectExternalRelations(RefinementCandidate refinement, TNodeTemplate detectorNode, TNodeTemplate matchingNode,
                                              TTopologyTemplate topology, Map<String, String> idMapping) {
        // if the current element is a staying element, the external elements do not need to be redirected
        return this.getStayMappingsOfCurrentElement((OTTopologyFragmentRefinementModel) refinement.getRefinementModel(), detectorNode)
            .findFirst().isPresent()
            ||
            this.getExternalRelations(matchingNode, refinement, topology)
                .allMatch(relationship -> {
                    List<OTRelationMapping> applicableRelMaps = refinement.getRefinementModel().getRelationMappings()
                        .stream()
                        // use anyMatch to reduce runtime
                        .filter(mapping -> mapping.getDetectorElement().getId().equals(detectorNode.getId()))
                        .filter(mapping -> (mapping.getDirection() == OTRelationDirection.INGOING
                                && relationship.getTargetElement().getRef().getId().equals(matchingNode.getId())
                            ) || (mapping.getDirection() == OTRelationDirection.OUTGOING
                                && relationship.getSourceElement().getRef().getId().equals(matchingNode.getId()))
                        )
                        .filter(mapping -> RefinementUtils.canRedirectRelation(mapping, relationship, relationshipTypes, nodeTypes))
                        .toList();

                    if (applicableRelMaps.size() == 0) {
                        return false;
                    }

                    OTRelationMapping relationMapping = applicableRelMaps.get(0);
                    if (applicableRelMaps.size() > 1) {
                        Optional<OTRelationMapping> moreConcreteRedirect = applicableRelMaps.stream()
                            .filter(mapping -> {
                                if (mapping.getValidSourceOrTarget() != null) {
                                    if (mapping.getDirection() == OTRelationDirection.INGOING) {
                                        return ModelUtilities.isOfType(mapping.getValidSourceOrTarget(), relationship.getSourceElement().getRef().getType(), nodeTypes);
                                    } else {
                                        return ModelUtilities.isOfType(mapping.getValidSourceOrTarget(), relationship.getTargetElement().getRef().getType(), nodeTypes);
                                    }
                                }
                                return false;
                            })
                            .findFirst();
                        if (moreConcreteRedirect.isPresent()) {
                            relationMapping = moreConcreteRedirect.get();
                        }
                    }
                    return redirectRelation(relationMapping, relationship, topology, idMapping, this.relationshipTypes, this.nodeTypes);
                });
    }

    private void redirectInternalRelations(OTTopologyFragmentRefinementModel prm, TNodeTemplate currentDetectorNode,
                                           TNodeTemplate matchingNodeInTopology, TTopologyTemplate topology, Map<String, String> idMapping) {
        if (prm.getStayMappings() != null) {
            this.getStayMappingsOfCurrentElement(prm, currentDetectorNode)
                .forEach(staying -> {
                    prm.getRefinementTopology().getRelationshipTemplates()
                        .forEach(refinementRelation -> {
                            if (refinementRelation.getSourceElement().getRef().getId().equals(staying.getRefinementElement().getId())) {
                                String addedRelationsID = idMapping.get(refinementRelation.getId());
                                TRelationshipTemplate addedRelation = topology.getRelationshipTemplate(addedRelationsID);

                                if (addedRelation == null) {
                                    LOGGER.error("Something went terribly wrong! Added Relation was not found...");
                                    return;
                                }

                                addedRelation.getSourceElement().setRef(matchingNodeInTopology);
                            } else if (refinementRelation.getTargetElement().getRef().getId().equals(staying.getRefinementElement().getId())) {
                                String addedRelationsID = idMapping.get(refinementRelation.getId());
                                TRelationshipTemplate addedRelation = topology.getRelationshipTemplate(addedRelationsID);

                                if (addedRelation == null) {
                                    LOGGER.error("Something went terribly wrong! Added Relation was not found...");
                                    return;
                                }

                                addedRelation.getTargetElement().setRef(matchingNodeInTopology);
                            }
                        });
                });
        }
    }

    public Stream<TRelationshipTemplate> getExternalRelations(TNodeTemplate matchingNode, RefinementCandidate candidate, TTopologyTemplate topology) {
        return topology.getRelationshipTemplates().stream()
            .filter(relationship ->
                // all relationships which have the matchingNode as source or target
                // -> \pi_1(rm_x) = \pi_2(sgm_i)
                matchingNode.getId().equals(relationship.getSourceElement().getRef().getId()) ||
                    matchingNode.getId().equals(relationship.getTargetElement().getRef().getId())
            ).filter(relationship -> {
                // ignore all relationships which are part of the sub-graph
                // !\exists sgm_y \in SGMs : \pi_1(sgm_y) = r_j
                return candidate.getDetectorGraph().edgeSet()
                    .stream()
                    .noneMatch(toscaEdge -> {
                        ToscaEdge edgeCorrespondence = candidate.getGraphMapping().getEdgeCorrespondence(toscaEdge, false);
                        return edgeCorrespondence.getTemplate().equals(relationship);
                    });
            });
    }

    private Map<String, Map<String, Integer>> calculateNewPositions(ToscaGraph detectorGraph, GraphMapping<ToscaNode, ToscaEdge> mapping, TTopologyTemplate refinementStructure) {
        HashMap<String, Map<String, Integer>> coordinates = new HashMap<>();
        int[] topLeftOriginal = {-1, -1};
        int[] topLeftReplacement = {-1, -1};

        detectorGraph.vertexSet().forEach(toscaNode -> {
            ToscaNode node = mapping.getVertexCorrespondence(toscaNode, false);
            getTopLeft(node.getTemplate().getX(), node.getTemplate().getY(), topLeftOriginal);
        });
        refinementStructure.getNodeTemplates().forEach(
            tNodeTemplate -> getTopLeft(tNodeTemplate.getX(), tNodeTemplate.getY(), topLeftReplacement)
        );

        refinementStructure.getNodeTemplates().forEach(nodeTemplate -> {
            if (nodeTemplate.getX() != null && nodeTemplate.getY() != null) {
                int x = Integer.parseInt(nodeTemplate.getX());
                int y = Integer.parseInt(nodeTemplate.getY());

                HashMap<String, Integer> newCoordinates = new HashMap<>();
                newCoordinates.put("x", (x - topLeftReplacement[0]) + topLeftOriginal[0]);
                newCoordinates.put("y", (y - topLeftReplacement[1]) + topLeftOriginal[1]);

                coordinates.put(nodeTemplate.getId(), newCoordinates);
            }
        });

        return coordinates;
    }

    private void getTopLeft(String stringX, String stringY, int[] topLeft) {
        int x = Integer.parseInt(stringX);
        int y = Integer.parseInt(stringY);

        if (topLeft[0] > x || topLeft[0] == -1) {
            topLeft[0] = x;
        }
        if (topLeft[1] > y || topLeft[1] == -1) {
            topLeft[1] = y;
        }
    }

    private Stream<OTStayMapping> getStayMappingsOfCurrentElement(OTTopologyFragmentRefinementModel prm, TEntityTemplate currentDetectorNode) {
        return prm.getStayMappings() == null ? Stream.of() :
            prm.getStayMappings().stream()
                .filter(stayMapping -> stayMapping.getDetectorElement().getId().equals(currentDetectorNode.getId()));
    }
}
