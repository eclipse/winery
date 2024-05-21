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
package org.eclipse.winery.repository.yaml.converter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import org.eclipse.winery.model.converter.support.Defaults;
import org.eclipse.winery.model.converter.support.Namespaces;
import org.eclipse.winery.model.ids.EncodingUtil;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.tosca.HasInheritance;
import org.eclipse.winery.model.tosca.TActivityDefinition;
import org.eclipse.winery.model.tosca.TArtifact;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.TCallOperationActivityDefinition;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TCapabilityDefinition;
import org.eclipse.winery.model.tosca.TCapabilityType;
import org.eclipse.winery.model.tosca.TDataType;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TEventFilterDefinition;
import org.eclipse.winery.model.tosca.TGroupDefinition;
import org.eclipse.winery.model.tosca.TGroupType;
import org.eclipse.winery.model.tosca.TImplementation;
import org.eclipse.winery.model.tosca.TImplementationArtifact;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TInterfaceDefinition;
import org.eclipse.winery.model.tosca.TInterfaceType;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TOperationDefinition;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TPolicyType;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TRequirementDefinition;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.model.tosca.TSchema;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.TTriggerDefinition;
import org.eclipse.winery.model.tosca.TWorkflow;
import org.eclipse.winery.model.tosca.extensions.kvproperties.AttributeDefinition;
import org.eclipse.winery.model.tosca.extensions.kvproperties.ConstraintClauseKV;
import org.eclipse.winery.model.tosca.extensions.kvproperties.ParameterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTActivityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.YTArtifactType;
import org.eclipse.winery.model.tosca.yaml.YTAttributeDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCallOperationActivityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityAssignment;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityType;
import org.eclipse.winery.model.tosca.yaml.YTConstraintClause;
import org.eclipse.winery.model.tosca.yaml.YTDataType;
import org.eclipse.winery.model.tosca.yaml.YTEntityType;
import org.eclipse.winery.model.tosca.yaml.YTGroupDefinition;
import org.eclipse.winery.model.tosca.yaml.YTGroupType;
import org.eclipse.winery.model.tosca.yaml.YTImplementation;
import org.eclipse.winery.model.tosca.yaml.YTImportDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceType;
import org.eclipse.winery.model.tosca.yaml.YTNodeTemplate;
import org.eclipse.winery.model.tosca.yaml.YTNodeType;
import org.eclipse.winery.model.tosca.yaml.YTOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.YTParameterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyType;
import org.eclipse.winery.model.tosca.yaml.YTPropertyAssignment;
import org.eclipse.winery.model.tosca.yaml.YTPropertyAssignmentOrDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPropertyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipTemplate;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipType;
import org.eclipse.winery.model.tosca.yaml.YTRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.YTRequirementDefinition;
import org.eclipse.winery.model.tosca.yaml.YTSchemaDefinition;
import org.eclipse.winery.model.tosca.yaml.YTServiceTemplate;
import org.eclipse.winery.model.tosca.yaml.YTTopologyTemplateDefinition;
import org.eclipse.winery.model.tosca.yaml.YTTriggerDefinition;
import org.eclipse.winery.model.tosca.yaml.YTWorkflow;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;
import org.eclipse.winery.model.tosca.yaml.support.ValueHelper;
import org.eclipse.winery.model.tosca.yaml.support.YTMapActivityDefinition;
import org.eclipse.winery.model.tosca.yaml.support.YTMapRequirementAssignment;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.yaml.converter.support.AssignmentBuilder;
import org.eclipse.winery.repository.yaml.converter.support.InheritanceUtils;
import org.eclipse.winery.repository.yaml.converter.support.TypeConverter;
import org.eclipse.winery.repository.yaml.converter.support.extension.YTImplementationArtifactDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToCanonical {

    public final static Logger LOGGER = LoggerFactory.getLogger(ToCanonical.class);
    private final IRepository context;
    private YTServiceTemplate root;
    private YTNodeTemplate currentNodeTemplate;
    private String currentNodeTemplateName;
    private String namespace;
    private List<TNodeTypeImplementation> nodeTypeImplementations;
    private List<TRelationshipTypeImplementation> relationshipTypeImplementations;
    private Map<String, TArtifactTemplate> artifactTemplates;
    private List<TRequirementType> requirementTypes;
    private List<TImport> imports;
    private Map<String, List<TPolicy>> policies;
    private Map<String, Map.Entry<String, String>> relationshipSTMap;
    private Map<String, TNodeTemplate> nodeTemplateMap;
    private AssignmentBuilder assignmentBuilder;

    public ToCanonical(IRepository context) {
        this.context = context;
    }

    private void reset() {
        this.nodeTypeImplementations = new ArrayList<>();
        this.relationshipTypeImplementations = new ArrayList<>();
        this.artifactTemplates = new LinkedHashMap<>();
        this.requirementTypes = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.policies = new LinkedHashMap<>();
        this.relationshipSTMap = new LinkedHashMap<>();
        this.nodeTemplateMap = new LinkedHashMap<>();
        this.currentNodeTemplate = null;
        this.currentNodeTemplateName = null;
        //this.interfaceTypes = new LinkedHashMap<>();
    }

    /**
     * Processes knowledge from TServiceTemplate needed to construct XML result
     */
    private void init(YTServiceTemplate node) {
        // no interface type for xml -> interface type information inserted into interface definitions
        //convert(node.getInterfaceTypes());
        this.assignmentBuilder = new AssignmentBuilder(new LinkedHashMap<>());
    }

    /**
     * Converts TOSCA YAML ServiceTemplates to Canonical TOSCA Definitions
     *
     * @return Canonical TOSCA Definitions
     */
    @NonNull
    public TDefinitions convert(YTServiceTemplate node, String id, String target_namespace, boolean isServiceTemplate) {
        if (node == null) {
            return new TDefinitions();
        }
        this.root = node;

        // Reset
        this.reset();
        this.namespace = target_namespace;

        init(node);

        TDefinitions.Builder builder = new TDefinitions.Builder(id + "_Definitions", target_namespace);
        builder.setImport(convert(node.getImports()))
                .addTypes(convert(node.getGroupTypes()));
        if (isServiceTemplate) {
            builder.addServiceTemplates(convertServiceTemplate(node, id, target_namespace));
        }
        builder.addNodeTypes(convert(node.getNodeTypes()))
                .addDataTypes(convert(node.getDataTypes()))
                .addNodeTypeImplementations(this.nodeTypeImplementations)
                .addRelationshipTypes(convert(node.getRelationshipTypes()))
                .addRelationshipTypeImplementations(this.relationshipTypeImplementations)
                .addCapabilityTypes(convert(node.getCapabilityTypes()))
                .addArtifactTypes(convert(node.getArtifactTypes()))
                .addArtifactTemplates(new ArrayList<>(this.artifactTemplates.values()))
                .addPolicyTypes(convert(node.getPolicyTypes()))
                .addInterfaceTypes(convert(node.getInterfaceTypes()))
                .setName(id)
                .addImports(this.imports)
                .addRequirementTypes(this.requirementTypes)
                .addGroupTypes(convert(node.getGroupTypes()));
        // WriterUtils.storeDefinitions(definitions, true, path);
        return builder.build();
    }

    /**
     * Converts TOSCA YAML ServiceTemplates to TOSCA XML ServiceTemplates
     *
     * @param node TOSCA YAML ServiceTemplate
     * @return TOSCA XML ServiceTemplate
     */
    @Nullable
    private TServiceTemplate convertServiceTemplate(YTServiceTemplate node, String id, String targetNamespace) {
        if (node == null) {
            return null;
        }

        TServiceTemplate result = new TServiceTemplate.Builder(id, convert(node.getTopologyTemplate()))
                .addDocumentation(node.getDescription())
                .setBoundaryDefinitions(
                        new TBoundaryDefinitions.Builder()
                                .addPolicies(this.policies.get("boundary")).build()
                )
                .setName(id)
                .setTargetNamespace(targetNamespace)
                .build();
        if (node.getTopologyTemplate() != null) {
            enhanceTopology(result.getTopologyTemplate(), node.getTopologyTemplate().getNodeTemplates());
        }
        return result;
    }

    /**
     * Converts TOSCA YAML EntityTypes to TOSCA XML EntityTypes
     * <p>
     * Additional element version added to tag. Missing elements abstract, final will not be set. Missing element
     * targetNamespace is searched in metadata
     *
     * @param node TOSCA YAML EntityType
     * @return TOSCA XML EntityType
     */
    private <T extends TEntityType.Builder<T>> T fillEntityTypeProperties(YTEntityType node, T builder) {
        builder.addDocumentation(node.getDescription())
                .setDerivedFrom(node.getDerivedFrom())
                .addTags(convertMetadata(node.getMetadata(), "targetNamespace", "abstract", "final"))
                .setTargetNamespace(node.getMetadata().get("targetNamespace"))
                .setAbstract(Boolean.parseBoolean(node.getMetadata().get("abstract")))
                .setFinal(Boolean.parseBoolean(node.getMetadata().get("final")))
                .setAttributeDefinitions(convert(node.getAttributes()));

        if (node.getVersion() != null) {
            String version = node.getVersion().getVersion();
            if (version != null) {
                builder.addTag(
                        new TTag.Builder("version", version)
                                .build()
                );
            }
        }

        if (!node.getProperties().isEmpty()) {
            builder.setProperties(convertProperties(node.getProperties()));
        }

//        if (!node.getProperties().isEmpty()) {
//            builder.addAny(convertWineryPropertiesDefinition(node.getProperties(), builder.build().getTargetNamespace(), builder.build().getIdFromIdOrNameField()));
//        }

        return builder;
    }

    /**
     * converts TOSCA YAML constraints to Winery XML constraints
     *
     * @param constraint TOSCA YAML constrains
     * @return Winery XML constraint
     */
    private ConstraintClauseKV convert(YTConstraintClause constraint) {
        ConstraintClauseKV con = new ConstraintClauseKV();
        con.setKey(constraint.getKey());
        con.setValue(constraint.getValue());
        con.setList(constraint.getList());
        return con;
    }

    /**
     * Converts TOSCA YAML metadata to TOSCA XML Tags
     *
     * @param metadata map of strings
     * @return TOSCA XML Tags
     */
    @NonNull
    private List<TTag> convertMetadata(Metadata metadata, String... excludedKeys) {
        Set<String> exclusionSet = new HashSet<>(Arrays.asList(excludedKeys));
        return metadata.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(e -> !exclusionSet.contains(e.getKey()))
                .map(entry ->
                        new TTag.Builder(entry.getKey(), entry.getValue())
                                .build())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Converts TOSCA YAML ArtifactTypes to TOSCA XML ArtifactTypes. Both objects have a super type EntityType.
     * Additional elements mime_type and file_ext from TOSCA YAML are moved to Tags in TOSCA XML
     *
     * @param node the YAML ArtifactType
     * @return TOSCA XML ArtifactType
     */
    private TArtifactType convert(YTArtifactType node, String id) {
        if (node == null) {
            return null;
        }
        String typeName = fixNamespaceDuplication(id, node.getMetadata().get("targetNamespace"));
        TArtifactType.Builder builder = new TArtifactType.Builder(typeName);
        fillEntityTypeProperties(node, builder);
        builder.setFileExtensions(node.getFileExt());
        if (node.getMimeType() != null) {
            builder.setMimeType(node.getMimeType());
        }
        return builder.build();
    }

    /**
     * Converts a TOSCA YAML ArtifactDefinition to a TOSCA XML ArtifactTemplate
     *
     * @param node TOSCA YAML ArtifactDefinition
     * @return TOSCA XML ArtifactTemplate
     */
    @NonNull
    @Deprecated
    private TArtifactTemplate convert(YTArtifactDefinition node, String id) {
        TArtifactTemplate.Builder builder = new TArtifactTemplate.Builder(id, node.getType());
        if (node.getFile() != null) {
            builder.addArtifactReferences(Collections.singletonList(new TArtifactReference.Builder(node.getFile()).build()));
        }
        if (node.getProperties() != null) {
            builder.setProperties(convertPropertyAssignments(node.getProperties()));
        }
        return builder.build();
    }

    /**
     * Converts a TOSCA YAML ArtifactDefinition to a non-TOSCA XML TArtifact
     *
     * @param node TOSCA YAML ArtifactDefinition
     * @return TOSCA XML ArtifactTemplate
     */
    @NonNull
    private TArtifact convertToTArtifact(YTArtifactDefinition node, String id) {
        TArtifact.Builder builder = new TArtifact.Builder(id, node.getType())
                .setDescription(node.getDescription())
                .setDeployPath(node.getDeployPath())
                .setFile(node.getFile());

        if (node.getProperties() != null) {
            builder.setProperties(convertPropertyAssignments(node.getProperties()));
        }

        return new TArtifact(builder);
    }

    /**
     * Converts a TOSCA YAML TInterfaceType to a non-TOSCA XML TInterfaceType
     *
     * @param node TOSCA YAML TInterfaceType
     * @return TOSCA XML TInterfaceType
     */
    private TInterfaceType convertToTInterfaceType(YTInterfaceType node, String type) {
        Map<String, TOperationDefinition> ops = new HashMap<>();
        node.getOperations().forEach((key, value) -> ops.put(key, convert(value, key)));
        String typeName = fixNamespaceDuplication(type, node.getMetadata().get("targetNamespace"));

        TInterfaceType.Builder interfaceBuilder = new TInterfaceType.Builder(typeName)
                .setDerivedFrom(node.getDerivedFrom())
                .setDescription(node.getDescription())
                .setOperations(ops);
        node.getMetadata();

        if (Objects.nonNull(node.getMetadata().get("targetNamespace"))) {
            interfaceBuilder.setTargetNamespace(node.getMetadata().get("targetNamespace"));
        }
        return interfaceBuilder.build();
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML DeploymentArtifacts
     *
     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
     * @return TOSCA XML DeploymentArtifacts
     */
    @Deprecated
    private List<TDeploymentArtifact> convertDeploymentArtifacts(@NonNull Map<String, YTArtifactDefinition> artifactDefinitionMap, String targetNamespace) {
        if (artifactDefinitionMap.isEmpty()) {
            return null;
        }
        return artifactDefinitionMap.entrySet().stream()
                .filter(Objects::nonNull)
                .map(entry -> {
                    TArtifactTemplate artifactTemplate = convert(entry.getValue(), entry.getKey());
                    this.artifactTemplates.put(artifactTemplate.getId(), artifactTemplate);
                    return new TDeploymentArtifact.Builder(entry.getKey(), entry.getValue().getType())
                            .setArtifactRef(new QName(targetNamespace, artifactTemplate.getId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML ImplementationArtifacts
     *
     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
     * @return TOSCA XML ImplementationArtifacts
     */
    @Deprecated
    private List<TImplementationArtifact> convertImplementationArtifact(@NonNull Map<String, YTArtifactDefinition> artifactDefinitionMap, String targetNamespace) {
        if (artifactDefinitionMap.isEmpty()) {
            return null;
        }
        return artifactDefinitionMap.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry) && Objects.nonNull(entry.getValue()))
                .map(entry -> {
                    TArtifactTemplate artifactTemplate = convert(entry.getValue(), entry.getKey());
                    this.artifactTemplates.put(artifactTemplate.getId(), artifactTemplate);
                    return new TImplementationArtifact.Builder(entry.getValue().getType())
                            .setName(entry.getKey())
                            .setArtifactRef(new QName(targetNamespace, artifactTemplate.getId()))
                            .setInterfaceName(convertInterfaceName(entry.getValue()))
                            .setOperationName(convertOperationName(entry.getValue()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Nullable
    public String convertInterfaceName(@NonNull YTArtifactDefinition node) {
        if (node instanceof YTImplementationArtifactDefinition) {
            return ((YTImplementationArtifactDefinition) node).getInterfaceName();
        }
        return null;
    }

    @Nullable
    public String convertOperationName(@NonNull YTArtifactDefinition node) {
        if (node instanceof YTImplementationArtifactDefinition)
            return ((YTImplementationArtifactDefinition) node).getOperationName();
        return null;
    }

    /**
     * Converts TOSCA YAML NodeTypes to TOSCA XML NodeTypes
     *
     * @param node TOSCA YAML NodeType
     * @return TOSCA XML NodeType
     */
    private TNodeType convert(YTNodeType node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        String typeName = fixNamespaceDuplication(id, node.getMetadata().get("targetNamespace"));
        TNodeType.Builder builder = fillEntityTypeProperties(node, new TNodeType.Builder(typeName))
                .addRequirementDefinitions(convert(node.getRequirements()))
                .addCapabilityDefinitions(convert(node.getCapabilities()))
                .setInterfaceDefinitions(convert(node.getInterfaces()))
                .addArtifacts(convert(node.getArtifacts()));
        return builder.build();
    }

    private String fixNamespaceDuplication(String id, String ns) {
        if (ns == null) {
            LOGGER.debug("Attempting to fix namespace duplication without a namespace for id {}", id);
            return id;
        }
        if (id.contains(ns)) {
            return id.replace(ns + ".", "");
        }
        return id;
    }

    /**
     * Converts TOSCA YAML NodeTemplates to TOSCA XML NodeTemplates Additional TOSCA YAML element metadata is put into
     * TOSCA XML documentation element Additional TOSCA YAML elements directives and copy are not converted
     *
     * @param node TOSCA YAML NodeTemplate
     * @return TOSCA XML NodeTemplate
     */
    private TNodeTemplate convert(YTNodeTemplate node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        this.currentNodeTemplate = node;
        this.currentNodeTemplateName = id;
        TNodeTemplate.Builder builder = new TNodeTemplate.Builder(id, node.getType())
                .addDocumentation(node.getDescription())
                .addDocumentation(node.getMetadata())
                .setName(node.getMetadata().getOrDefault(Defaults.DISPLAY_NAME, id))
                .setX(node.getMetadata().getOrDefault(Defaults.X_COORD, "0"))
                .setY(node.getMetadata().getOrDefault(Defaults.Y_COORD, "0"))
                .setProperties(convertPropertyAssignments(node.getProperties()))
                .addRequirements(convert(node.getRequirements()))
                .addCapabilities(convert(node.getCapabilities()))
                // .setDeploymentArtifacts(convertDeploymentArtifacts(node.getArtifacts()));
                .setArtifacts(convert(node.getArtifacts()));
        TNodeTemplate nodeTemplate = builder.build();
        this.nodeTemplateMap.put(id, nodeTemplate);

        return nodeTemplate;
    }

    /**
     * Constructs the name of the PropertyType for a given type
     */
    private QName getPropertyTypeName(QName type) {
        return new QName(type.getNamespaceURI(), type.getLocalPart() + "_Properties");
    }

    /**
     * Converts TOSCA YAML RequirementDefinition to TOSCA XML RequirementDefinition
     *
     * @param node TOSCA YAML RequirementDefinition
     * @return TOSCA XML RequirementDefinition
     */
    private TRequirementDefinition convert(YTRequirementDefinition node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        // TOSCA YAML does not have RequirementTypes:
        // * construct TOSCA XML RequirementType from TOSCA YAML Requirement Definition	
        TRequirementDefinition.Builder builder = new TRequirementDefinition.Builder(id)
                .setLowerBound(node.getLowerBound())
                .setUpperBound(node.getUpperBound())
                .setCapability(node.getCapability())
                .setNode(node.getNode());

        if (node.getRelationship() != null) {
            builder = builder.setRelationship(node.getRelationship().getType());
        }

        return builder.build();
    }

    /**
     * Converts TOSCA YAML RequirementAssignments to TOSCA XML Requirements Additional TOSCA YAML elements node_filter
     * and occurrences are not converted
     *
     * @param node TOSCA YAML RequirementAssignments
     * @return return List of TOSCA XML Requirements
     */
    private TRequirement convert(YTRequirementAssignment node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        String reqId = this.currentNodeTemplateName + "_" + id;
        TRequirement.Builder builder = new TRequirement.Builder(reqId, id, null);

        if (node.getCapability() != null) {
            builder = builder.setCapability(node.getCapability().getLocalPart());
        } else {
            // when exporting, this must be caught, but while developing, it is tolerated
            // todo check if this is the case during export!
            LOGGER.error("TRequirementAssignment has no capability!");
        }

        if (node.getRelationship() != null && node.getRelationship().getType() != null) {
            builder = builder.setRelationship(node.getRelationship().getType().toString());
        }

        if (node.getNode() != null) {
            builder = builder.setNode(node.getNode().toString());
        }

        return builder.build();
    }

    private TCapability convert(YTCapabilityAssignment node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        String capId = this.currentNodeTemplateName + "_" + id;
        QName capType = this.getCapabilityTypeOfCapabilityName(id);
        TCapability.Builder builder = new TCapability.Builder(capId, capType, id);

        if (node.getProperties().entrySet().size() > 0) {
            TEntityTemplate.Properties toscaProperties = this.convertPropertyAssignments(node.getProperties());
            return builder.setProperties(toscaProperties).build();
        }

        return builder.build();
    }

    /**
     * Gets a Capability Definition corresponding to the passed capName such that it is the lowest in the type ancestry
     * of the corresponding nodeType. If no such Capability Definition is found, it returns null.
     */
    private TCapabilityDefinition getCapabilityDefinitionOfCapabilityName(String capName, QName nodeType) {
        List<HasInheritance> ancestry = InheritanceUtils.getInheritanceHierarchy(new NodeTypeId(nodeType), context);
        List<TCapabilityDefinition> currentCapDefs;

        for (HasInheritance currentNT : ancestry) {
            assert currentNT instanceof TNodeType;
            if (((TNodeType) currentNT).getCapabilityDefinitions() != null) {
                currentCapDefs = ((TNodeType) currentNT).getCapabilityDefinitions();

                for (TCapabilityDefinition currentDef : currentCapDefs) {
                    if (currentDef.getName().equals(capName)) {
                        return currentDef;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets the CapabilityType of a Capability identified by its name as present in the capability definition or
     * capability assignment
     */
    private QName getCapabilityTypeOfCapabilityName(String capName) {
        if (this.currentNodeTemplate != null) {
            QName nodeType = this.currentNodeTemplate.getType();
            TCapabilityDefinition capDef = this.getCapabilityDefinitionOfCapabilityName(capName, nodeType);

            if (capDef != null) {
                return capDef.getCapabilityType();
            }
        }

        return null;
    }

    /**
     * Converts TOSCA YAML CapabilityTypes to TOSCA XML CapabilityTypes
     *
     * @param node TOSCA YAML CapabilityType
     * @return TOSCA XML CapabilityType
     */
    private TCapabilityType convert(YTCapabilityType node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        String typeName = fixNamespaceDuplication(id, node.getMetadata().get("targetNamespace"));
        return fillEntityTypeProperties(node, new TCapabilityType.Builder(typeName))
                .setValidSourceTypes(node.getValidSourceTypes())
                .build();
    }

    /**
     * Converts TOSCA YAML CapabilityDefinitions to TOSCA XML CapabilityDefinitions Additional TOSCA YAML elements
     * properties, attributes and valid_source_types are not converted
     *
     * @param node TOSCA YAML CapabilityDefinition
     * @return TOSCA XML CapabilityDefinition
     */
    private TCapabilityDefinition convert(YTCapabilityDefinition node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }

        return new TCapabilityDefinition.Builder(id, node.getType())
                .addDocumentation(node.getDescription())
                .setLowerBound(node.getLowerBound())
                .setUpperBound(node.getUpperBound())
                .setValidSourceTypes(node.getValidSourceTypes())
                .build();
    }

    private TInterfaceDefinition convert(YTInterfaceDefinition node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        TInterfaceDefinition def = new TInterfaceDefinition();
        def.setId(id);
        def.setName(id);
        def.setType(node.getType());
        def.setInputs(convert(node.getInputs()));
        def.setOperations(convert(node.getOperations()));
        return def;
    }

    /**
     * Convert TOSCA YAML TopologyTemplatesDefinition to TOSCA XML TopologyTemplates Additional TOSCA YAML elements
     * inputs, outputs, groups, policies, substitution_mappings and workflows are not converted
     *
     * @param node TOSCA YAML TopologyTemplateDefinition
     * @return TOSCA XML TopologyTemplate
     */
    private TTopologyTemplate convert(YTTopologyTemplateDefinition node) {
        if (node == null) {
            return null;
        }

        TTopologyTemplate.Builder builder = new TTopologyTemplate.Builder();
        builder.addDocumentation(node.getDescription());

        builder.setNodeTemplates(convert(node.getNodeTemplates()));
        builder.setRelationshipTemplates(convert(node.getRelationshipTemplates()));
        builder.setPolicies(convert(node.getPolicies()));
        builder.setGroups(convert(node.getGroups()));

        if (node.getInputs() != null) {
            builder.setInputs(convert(node.getInputs()));
        }
        if (node.getOutputs() != null) {
            builder.setOutputs(convert(node.getOutputs()));
        }

        if (node.getWorkflows() != null) {
            builder.setWorkflows(convert(node.getWorkflows()));
        }

        return builder.build();
    }

    /**
     * Determines and updates the source and target for relationship templates in the given canonical topology based on
     * the yaml node template definitions. Relationships are determined through the requirements of the given node
     * templates.
     *
     * @param topology      A canonical model TopologyTemplate
     * @param nodeTemplates The node templates of the yaml topology template that was originally converted into the
     *                      <tt>topology</tt>
     */
    private void enhanceTopology(TTopologyTemplate topology, @NonNull Map<String, YTNodeTemplate> nodeTemplates) {
        if (topology == null) {
            return;
        }
        nodeTemplates.forEach((id, nt) -> {
            @NonNull List<YTMapRequirementAssignment> reqs = nt.getRequirements();
            if (reqs.isEmpty()) {
                return;
            }
            for (YTMapRequirementAssignment map : reqs) {
                for (Map.Entry<String, YTRequirementAssignment> data : map.getMap().entrySet()) {
                    final YTRequirementAssignment req = data.getValue();
                    TRelationshipTemplate relationship = topology.getRelationshipTemplate(req.getRelationship().getType().toString());
                    if (relationship == null) {
                        // requirement with a type that is not a RelationshipTemplate in the topology
                        continue;
                    }
                    relationship.setTargetNodeTemplate(topology.getNodeTemplate(req.getNode().toString()));
                    relationship.setSourceNodeTemplate(topology.getNodeTemplate(id));
                }
            }
        });
    }

    /**
     * Converts TOSCA YAML RelationshipTypes to TOSCA XML RelationshipTypes
     *
     * @param node TOSCA YAML RelationshipType
     * @return TOSCA XML RelationshipType
     */
    private TRelationshipType convert(YTRelationshipType node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        String typeName = fixNamespaceDuplication(id, node.getMetadata().get("targetNamespace"));
        // convertRelationshipTypeImplementation(node.getInterfaces(), id, node.getMetadata().get("targetNamespace"));
        return fillEntityTypeProperties(node, new TRelationshipType.Builder(typeName))
                .addInterfaces(convert(node.getInterfaces(), null))
                .addSourceInterfaces(convert(node.getInterfaces(), "SourceInterfaces"))
                .addTargetInterfaces(convert(node.getInterfaces(), "TargetInterfaces"))
                .setInterfaceDefinitions(convert(node.getInterfaces()))
                // yaml Relationship Types do not contain valid sources
                //.setValidSource(convertValidTargetSource(node.getValidTargetTypes(), true))
                .setValidTarget(convertValidTargetSource(node.getValidTargetTypes(), false))
                .setValidTargetList(node.getValidTargetTypes())
                .build();
    }

    private QName convertValidTargetSource(List<QName> targets, Boolean isSource) {
        if (targets != null) {
            if (targets.size() > 1) {
                if (isSource) {
                    return targets.get(0);
                } else {
                    return targets.get(1);
                }
            }
        }
        return null;
    }

    /**
     * Converts TOSCA YAML InterfaceDefinitions to TOSCA XML Interface Additional TOSCA YAML element input with
     * PropertyAssignment or PropertyDefinition is not converted
     *
     * @return TOSCA XML Interface
     */
    private List<TInterface> convert(Map<String, YTInterfaceDefinition> nodes, String type) {
        List<TInterface> output = new ArrayList<>();
        for (Map.Entry<String, YTInterfaceDefinition> node : nodes.entrySet()) {
            // FIXME Fix interface conversion!?
            if (type == null && node.getValue().getType() == null) {
                //output.add(convert(node.getValue(), node.getKey()));
            } else if (type != null && node.getValue().getType() != null) {
                if (node.getValue().getType().getLocalPart().equalsIgnoreCase(type)) {
                    //output.add(convert(node.getValue(), node.getKey()));
                }
            }
        }
        return output;
    }

    /**
     * Converts TOSCA YAML RelationshipTemplate to TOSCA XML RelationshipTemplate Additional TOSCA YAML element
     * interfaces is not converted
     *
     * @param node TOSCA YAML RelationshipTemplate
     * @return TOSCA XML RelationshipTemplate
     */
    private TRelationshipTemplate convert(YTRelationshipTemplate node, String id) {
        if (node == null) {
            return null;
        }

        // the topology modeler finds the source and target of relationships
        return new TRelationshipTemplate.Builder(id, node.getType(), null, null)
                .setName(node.getType().getLocalPart())
                .setProperties(convertPropertyAssignments(node.getProperties()))
                .build();
    }

    /**
     * Converts TOSCA YAML PolicyTypes to TOSCA XML  PolicyTypes Additional TOSCA YAML element triggers is not
     * converted
     *
     * @param node TOSCA YAML PolicyType
     * @return TOSCA XML PolicyType
     */
    private TPolicyType convert(YTPolicyType node, String id) {
        if (node == null) {
            return null;
        }
        String typeName = fixNamespaceDuplication(id, node.getMetadata().get("targetNamespace"));
        TPolicyType.Builder builder = new TPolicyType.Builder(typeName);
        fillEntityTypeProperties(node, builder);
        builder.setAppliesTo(convertTargets(node.getTargets()));
        builder.setTriggers(
                node.getTriggers().entrySet()
                        .stream()
                        .map(
                                entry -> {
                                    if (entry.getValue() != null) {
                                        return convert(entry.getValue(), entry.getKey());
                                    } else {
                                        return null;
                                    }
                                }
                        )
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        return builder.build();
    }

    public TTriggerDefinition convert(YTTriggerDefinition node, String id) {
        TTriggerDefinition.Builder defBuilder = new TTriggerDefinition.Builder(id, node.getEvent());
        if (Objects.nonNull(node.getDescription())) {
            defBuilder.description(node.getDescription());
        }
        if (Objects.nonNull(node.getTargetFilter())) {
            TEventFilterDefinition.Builder filterBuilder = new TEventFilterDefinition.Builder(node.getTargetFilter().getNode());
            if (Objects.nonNull(node.getTargetFilter().getCapability())) {
                filterBuilder.capability(node.getTargetFilter().getCapability());
            }
            if (Objects.nonNull(node.getTargetFilter().getRequirement())) {
                filterBuilder.requirement(node.getTargetFilter().getRequirement());
            }
            defBuilder.targetFilter(filterBuilder.build());
        }
        if (Objects.nonNull(node.getAction())) {
            defBuilder.action(
                    node.getAction().stream()
                            .map(this::convert)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        }

        return defBuilder.build();
    }

    public TActivityDefinition convert(YTMapActivityDefinition node) {
        if (Objects.isNull(node) || node.getMap().isEmpty()) {
            return null;
        }

        for (YTActivityDefinition value : node.getMap().values()) {
            if (value instanceof YTCallOperationActivityDefinition) {
                YTCallOperationActivityDefinition yamlDef = (YTCallOperationActivityDefinition) value;
                TCallOperationActivityDefinition canonicalDef = new TCallOperationActivityDefinition(yamlDef.getOperation());
                List<ParameterDefinition> inputs = yamlDef.getInputs().entrySet()
                        .stream()
                        .map(entry -> convert(entry.getValue(), entry.getKey()))
                        .collect(Collectors.toList());
                canonicalDef.setInputs(inputs);

                return canonicalDef;
            }
        }
        return null;
    }

    /**
     * Converts a TOSCA YAML PolicyDefinitions to a TOSCA XML Policy. trigger and metadata are not converted
     *
     * @param node TOSCA YAML PolicyDefinition
     */
    private TPolicy convert(YTPolicyDefinition node, String id) {
        if (node == null) {
            return null;
        }

        TPolicy.Builder builder = new TPolicy
                .Builder(node.getType())
                .setName(id)
                .addDocumentation(node.getDescription())
                .setTargets(node.getTargets());

        if (node.getProperties().entrySet().size() > 0) {
            Map<String, YTPropertyAssignment> originalProperties = node.getProperties();
            TEntityTemplate.Properties toscaProperties = this.convertPropertyAssignments(originalProperties);
            return builder.setProperties(toscaProperties).build();
        }

        return builder.build();
    }

    private TEntityTemplate.Properties convertPropertyAssignments(Map<String, YTPropertyAssignment> originalProperties) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        // don't stringify values here, that'd lose type information
        originalProperties.forEach((key, value) -> properties.put(key, value.getValue()));
        TEntityTemplate.YamlProperties toscaProperties = new TEntityTemplate.YamlProperties();
        toscaProperties.setProperties(properties);
        return toscaProperties;
    }

    /**
     * Adds TOSCA XML Policy to Map<String, TPolicy> policies
     *
     * @param target Key of the map
     */
    private void addPolicy(String target, TPolicy policy) {
        if (this.policies.containsKey(target)) {
            this.policies.get(target).add(policy);
        } else {
            List<TPolicy> policies = new ArrayList<>();
            policies.add(policy);
            this.policies.put(target, policies);
        }
    }

    private ParameterDefinition convert(YTParameterDefinition node, String name) {
        if (node == null) {
            return null;
        }
        ParameterDefinition p = new ParameterDefinition();
        p.setKey(name);
        p.setType(node.getType());
        p.setDescription(node.getDescription());
        p.setRequired(node.getRequired());
        p.setDefaultValue(ValueHelper.toString(node.getDefault()));
        p.setValue(ValueHelper.toString(node.getValue()));
        return p;
    }

    /**
     * Converts TOSCA YAML TImportDefinitions and returns list of TOSCA XML TImports
     */
    private TImport convert(YTImportDefinition node, String name) {
        String importType;
        if (node.getFile().endsWith(".tosca")) {
            importType = Namespaces.TOSCA_YAML_NS;
        } else {
            importType = name;
        }

        TImport.Builder builder = new TImport.Builder(importType)
                .setNamespace(node.getNamespaceUri())
                .setLocation(node.getFile());

        return builder.build();
    }

    private String getFileNameFromFile(String filename) {
        return filename.substring(filename.lastIndexOf(File.separator) + 1, filename.lastIndexOf("."));
    }

    /**
     * Convert A list of TOSCA YAML PolicyType targets to TOSCA XML PolicyType AppliesTo
     *
     * @param targetList list of TOSCA YAML PolicyType targets
     * @return TOSCA XML PolicyType AppliesTo
     */
    private List<TPolicyType.NodeTypeReference> convertTargets(List<QName> targetList) {
        if (targetList == null || targetList.isEmpty()) {
            return null;
        }

        return targetList.stream()
                .map(TPolicyType.NodeTypeReference::new)
                .collect(Collectors.toList());
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML NodeTypeImplementations and ArtifactTemplates
     */
    private void convertNodeTypeImplementation(
            Map<String, YTArtifactDefinition> implArtifacts,
            Map<String, YTArtifactDefinition> deploymentArtifacts, String type, String targetNamespace) {
        for (Map.Entry<String, YTArtifactDefinition> implArtifact : implArtifacts.entrySet()) {
            for (Map.Entry<String, YTArtifactDefinition> deploymentArtifact : deploymentArtifacts.entrySet()) {
                if (implArtifact.getKey().equalsIgnoreCase(deploymentArtifact.getKey())) {
                    deploymentArtifacts.remove(deploymentArtifact.getKey());
                }
            }
        }
        TNodeTypeImplementation.Builder builder = (new TNodeTypeImplementation.Builder(type + "_impl", new QName(targetNamespace, type))
                .setTargetNamespace(targetNamespace)
                // .setDeploymentArtifacts(convertDeploymentArtifacts(deploymentArtifacts, targetNamespace))
        );
        List<TImplementationArtifact> implementationArtifacts = convertImplementationArtifact(implArtifacts, targetNamespace);
        builder.setImplementationArtifacts(implementationArtifacts);
        this.nodeTypeImplementations.add(builder.build());
    }

    private TOperationDefinition convert(YTOperationDefinition node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        TOperationDefinition def = new TOperationDefinition();
        def.setId(id);
        def.setName(id);
        def.setDescription(node.getDescription());
        def.setInputs(convert(node.getInputs()));
        def.setOutputs(convert(node.getOutputs()));
        def.setImplementation(convert(node.getImplementation()));
        return def;
    }

    private TGroupDefinition convert(YTGroupDefinition node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }
        TGroupDefinition.Builder builder = new TGroupDefinition.Builder(id, node.getType())
                .setDescription(node.getDescription())
                .setMembers(node.getMembers())
                .setProperties(convertPropertyAssignments(node.getProperties()));
        return builder.build();
    }

    private TWorkflow convert(YTWorkflow node, String id) {
        if (Objects.isNull(node)) {
            return null;
        }

        TWorkflow.Builder builder = new TWorkflow.Builder(id)
                .setDescription(node.getDescription())
                .setOutputs(convert(node.getOutputs()))
                .setImplementation(convert(node.getImplementation()));

        if (node.getInputs().entrySet().size() > 0) {
            Map<String, YTPropertyAssignment> originalProperties = node.getInputs();
            TEntityTemplate.Properties toscaProperties = this.convertPropertyAssignments(originalProperties);
            builder.setInputs(toscaProperties).build();
        }

        return builder.build();
    }

    private TImplementation convert(YTImplementation node) {
        if (Objects.isNull(node)) {
            return null;
        }
        TImplementation def = new TImplementation();
        def.setPrimary(node.getPrimaryArtifactName());
        def.setDependencies(node.getDependencyArtifactNames());
        def.setTimeout(node.getTimeout());
        def.setOperationHost(node.getOperationHost());
        return def;
    }

    @Deprecated
    private List<TParameter> convertParameters(Map<String, YTPropertyAssignmentOrDefinition> node) {
        return node.entrySet().stream()
                .map(entry -> {
                    if (entry.getValue() instanceof YTPropertyDefinition) {
                        return convertParameter((YTPropertyDefinition) entry.getValue(), entry.getKey());
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Deprecated
    private TParameter convertParameter(YTPropertyDefinition node, String id) {
        return new TParameter.Builder(
                id,
                TypeConverter.INSTANCE.convert(node.getType()).getLocalPart(),
                node.getRequired()
        ).build();
    }

    public AttributeDefinition convert(YTAttributeDefinition node, String name) {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setKey(name);
        attribute.setType(node.getType());
        attribute.setDescription(node.getDescription());
        attribute.setDefaultValue(ValueHelper.toString(node.getDefault()));
        return attribute;
    }

    private TGroupType convert(YTGroupType node, String name) {
        if (Objects.isNull(node)) {
            return null;
        }
        TGroupType.Builder builder = fillEntityTypeProperties(node, new TGroupType.Builder(name))
                .setMembers(node.getMembers());
        return builder.build();
    }

    public TDataType convert(YTDataType node, String id) {
        String name = fixNamespaceDuplication(id, node.getMetadata().get("targetNamespace"));
        TDataType.Builder builder = new TDataType.Builder(name)
                // set specific fields
                .addConstraints(convertList(node.getConstraints(), this::convert));
        fillEntityTypeProperties(node, builder);
        TDataType result = builder.build();

        // FIXME need to actually transform the node.getProperties() to an xml schema
        //  to be able to import it and add a PropertiesDefinition reference to that schema
        String namespace = this.namespace;
        if (namespace == null) {
            // attempt to patch namespace with the definitions' targetNamespace
            namespace = result.getTargetNamespace();
        }
        if (namespace == null) {
            LOGGER.warn("Could not determine namespace for DataType {}. Imports may be incorrect!", id);
            return result;
        }
        TImport importDefinition = new TImport.Builder(Namespaces.XML_NS)
                .setLocation(EncodingUtil.URLencode(namespace) + ".xsd")
                // namespace must not be null
                .setNamespace(namespace)
                .build();
        if (!this.imports.contains(importDefinition)) {
            this.imports.add(importDefinition);
        }
        return result;
    }

    private TEntityType.YamlPropertiesDefinition convertProperties(@NonNull Map<String, YTPropertyDefinition> properties) {
        TEntityType.YamlPropertiesDefinition result = new TEntityType.YamlPropertiesDefinition();
        result.setProperties(convert(properties));
        return result;
    }

    @SuppressWarnings( {"unchecked"})
    private <V, T> List<T> convert(List<? extends Map<String, V>> node) {
        return node.stream()
                .flatMap(map -> map.entrySet().stream())
                .map((Map.Entry<String, V> entry) -> {
                    if (entry.getValue() instanceof YTImportDefinition && "file".equals(entry.getKey())) {
                        return (T) convert((YTImportDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTRequirementDefinition) {
                        return (T) convert((YTRequirementDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTRequirementAssignment) {
                        return (T) convert((YTRequirementAssignment) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTPolicyDefinition) {
                        return (T) convert((YTPolicyDefinition) entry.getValue(), entry.getKey());
                    } else {
                        V v = entry.getValue();
                        assert (v instanceof YTImportDefinition ||
                                v instanceof YTRequirementDefinition ||
                                v instanceof YTRequirementAssignment);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @SuppressWarnings( {"unchecked"})
    private <V, T> List<T> convert(@NonNull Map<String, V> map) {
        return map.entrySet().stream()
                .map((Map.Entry<String, V> entry) -> {
                    if (entry.getValue() == null) {
                        return null;
                    } else if (entry.getValue() instanceof YTRelationshipType) {
                        return convert((YTRelationshipType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTRelationshipTemplate) {
                        return convert((YTRelationshipTemplate) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTArtifactType) {
                        return convert((YTArtifactType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTArtifactDefinition) {
                        return convertToTArtifact((YTArtifactDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTCapabilityType) {
                        return convert((YTCapabilityType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTCapabilityDefinition) {
                        return convert((YTCapabilityDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTPolicyType) {
                        return convert((YTPolicyType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTRequirementDefinition) {
                        return convert((YTRequirementDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTInterfaceType) {
                        return convertToTInterfaceType((YTInterfaceType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTInterfaceDefinition) {
                        return convert((YTInterfaceDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTOperationDefinition) {
                        return convert((YTOperationDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTNodeTemplate) {
                        return convert((YTNodeTemplate) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTDataType) {
                        return convert((YTDataType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTGroupType) {
                        return convert((YTGroupType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTGroupDefinition) {
                        return convert((YTGroupDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTNodeType) {
                        return convert((YTNodeType) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTImportDefinition) {
                        return convert((YTImportDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTPolicyDefinition) {
                        return convert((YTPolicyDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTCapabilityAssignment) {
                        return convert((YTCapabilityAssignment) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTParameterDefinition) {
                        return convert((YTParameterDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTPropertyDefinition) {
                        return convert((YTPropertyDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTAttributeDefinition) {
                        return convert((YTAttributeDefinition) entry.getValue(), entry.getKey());
                    } else if (entry.getValue() instanceof YTWorkflow) {
                        return convert((YTWorkflow) entry.getValue(), entry.getKey());
                    } else {
                        V v = entry.getValue();
                        System.err.println(v);
                        assert (v instanceof YTRelationshipType ||
                                v instanceof YTRelationshipTemplate ||
                                v instanceof YTArtifactType ||
                                v instanceof YTArtifactDefinition ||
                                v instanceof YTCapabilityType ||
                                v instanceof YTCapabilityDefinition ||
                                v instanceof YTCapabilityAssignment ||
                                v instanceof YTPolicyType ||
                                v instanceof YTRequirementDefinition ||
                                //v instanceof TInterfaceType ||
                                v instanceof YTInterfaceDefinition ||
                                v instanceof YTOperationDefinition ||
                                v instanceof YTNodeTemplate ||
                                v instanceof YTGroupType ||
                                v instanceof YTNodeType ||
                                v instanceof YTImportDefinition ||
                                v instanceof YTPolicyDefinition
                        );
                        return null;
                    }
                })
                .flatMap(entry -> {
                    if (entry instanceof List) {
                        return ((List<T>) entry).stream();
                    } else {
                        return (Stream<T>) Stream.of(entry);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TEntityType.YamlPropertyDefinition convert(YTPropertyDefinition node, String name) {
        TEntityType.YamlPropertyDefinition.Builder builder = new TEntityType.YamlPropertyDefinition.Builder(name)
                .setType(node.getType())
                .setDescription(node.getDescription())
                .setRequired(node.getRequired())
                .setStatus(TEntityType.YamlPropertyDefinition.Status.getStatus(node.getStatus().toString()))
                .setConstraints(convertList(node.getConstraints(), this::convert))
                .setEntrySchema(convert(node.getEntrySchema()))
                .setKeySchema(convert(node.getKeySchema()));
        // special handling to keep empty string values
        if (node.getType() != null && node.getType().toString().equals("string")
                && node.getDefault() != null && StringUtils.isEmpty(node.getDefault().toString())) {
            builder.setDefaultValue("\"\"");
        } else {
            builder.setDefaultValue(ValueHelper.toString(node.getDefault()));
        }
        return builder.build();
    }

    @Nullable
    private TSchema convert(@Nullable YTSchemaDefinition node) {
        if (node == null) {
            return null;
        }
        TSchema.Builder builder = new TSchema.Builder(node.getType());
        return builder.setConstraints(convertList(node.getConstraints(), this::convert))
                .setDescription(node.getDescription())
                .setEntrySchema(convert(node.getEntrySchema()))
                .setKeySchema(convert(node.getKeySchema()))
                .build();
    }

    private <R, I> List<R> convertList(@Nullable List<I> yaml, Function<I, R> convert) {
        if (yaml == null) {
            return Collections.emptyList();
        }
        return yaml.stream().map(convert).collect(Collectors.toList());
    }
}
