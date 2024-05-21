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
package org.eclipse.winery.repository.converter.writer;

import org.eclipse.winery.model.tosca.yaml.YTArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.YTArtifactType;
import org.eclipse.winery.model.tosca.yaml.YTAttributeAssignment;
import org.eclipse.winery.model.tosca.yaml.YTAttributeDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCallOperationActivityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityAssignment;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityType;
import org.eclipse.winery.model.tosca.yaml.YTConstraintClause;
import org.eclipse.winery.model.tosca.yaml.YTDataType;
import org.eclipse.winery.model.tosca.yaml.YTEntityType;
import org.eclipse.winery.model.tosca.yaml.YTEventFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTGroupDefinition;
import org.eclipse.winery.model.tosca.yaml.YTGroupType;
import org.eclipse.winery.model.tosca.yaml.YTImplementation;
import org.eclipse.winery.model.tosca.yaml.YTImportDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceAssignment;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceType;
import org.eclipse.winery.model.tosca.yaml.YTNodeFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTNodeTemplate;
import org.eclipse.winery.model.tosca.yaml.YTNodeType;
import org.eclipse.winery.model.tosca.yaml.YTOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.YTParameterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyType;
import org.eclipse.winery.model.tosca.yaml.YTPropertyAssignment;
import org.eclipse.winery.model.tosca.yaml.YTPropertyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPropertyFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipAssignment;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipTemplate;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipType;
import org.eclipse.winery.model.tosca.yaml.YTRepositoryDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.YTRequirementDefinition;
import org.eclipse.winery.model.tosca.yaml.YTSchemaDefinition;
import org.eclipse.winery.model.tosca.yaml.YTServiceTemplate;
import org.eclipse.winery.model.tosca.yaml.YTStatusValue;
import org.eclipse.winery.model.tosca.yaml.YTSubstitutionMappings;
import org.eclipse.winery.model.tosca.yaml.YTTopologyTemplateDefinition;
import org.eclipse.winery.model.tosca.yaml.YTTriggerDefinition;
import org.eclipse.winery.model.tosca.yaml.YTVersion;
import org.eclipse.winery.model.tosca.yaml.YTWorkflow;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;
import org.eclipse.winery.model.tosca.yaml.support.YTListString;
import org.eclipse.winery.model.tosca.yaml.support.YTMapActivityDefinition;
import org.eclipse.winery.model.tosca.yaml.support.YTMapImportDefinition;
import org.eclipse.winery.model.tosca.yaml.support.YTMapObject;
import org.eclipse.winery.model.tosca.yaml.support.YTMapPropertyFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.support.YTMapRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.support.YTMapRequirementDefinition;
import org.eclipse.winery.model.tosca.yaml.support.YamlSpecKeywords;
import org.eclipse.winery.model.tosca.yaml.tosca.datatypes.Credential;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractVisitor;
import org.eclipse.winery.model.tosca.yaml.visitor.VisitorNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

// FIXME this belongs with the specific implementation
public class YamlWriter extends AbstractVisitor<YamlPrinter, YamlWriter.Parameter> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractVisitor.class);

    private static final String[] PROPERTY_FUNCTIONS = new String[] {
            "get_input", "get_property", "get_attribute", "get_operation_output", "get_nodes_of_type", "get_artifact"
    };

    private final int INDENT_SIZE;

    public YamlWriter() {
        this.INDENT_SIZE = 2;
    }

    public YamlWriter(int indentSize) {
        this.INDENT_SIZE = indentSize;
    }

    public InputStream writeToInputStream(YTServiceTemplate serviceTemplate) {
        try {
            String output = this.visit(serviceTemplate, new Parameter(0)).toString();
            return new ByteArrayInputStream(output.getBytes());
        } catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage());
        }
        return null;
    }

    public void write(YTServiceTemplate serviceTemplate, Path fileName) {
        Objects.requireNonNull(serviceTemplate);
        try {
            Files.createDirectories(Objects.requireNonNull(fileName).getParent());
        } catch (IOException e) {
            LOGGER.debug("Could not create directory", e);
            return;
        }
        try (FileWriter fileWriter = new FileWriter(fileName.toFile())) {
            fileWriter.write(this.visit(serviceTemplate, new Parameter(0)).toString());
        } catch (IOException e) {
            LOGGER.debug("Could write to file", e);
        }
    }

    /**
     * @deprecated Use {@link YamlWriter#write(YTServiceTemplate, Path)}
     */
    @Deprecated
    public void write(YTServiceTemplate serviceTemplate, String fileName) {
        this.write(serviceTemplate, Paths.get(fileName));
    }

    public YamlPrinter visit(YTServiceTemplate node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TOSCA_DEF_VERSION, node.getToscaDefinitionsVersion())
                .printNewLine()
                .print(node.getMetadata().accept(this, parameter))
                .print(printList(YamlSpecKeywords.IMPORTS,
                        node.getImports().stream().map(YTMapImportDefinition::values).flatMap(Collection::stream).collect(Collectors.toList()),
                        parameter))
                // .printKeyValue("description", node.getDescription())
                .print(printMapObject(YamlSpecKeywords.DSL_DEFINITIONS, node.getDslDefinitions(), parameter))
                .print(printMap(YamlSpecKeywords.REPOSITORIES, node.getRepositories(), parameter))
                .print(printMap(YamlSpecKeywords.ARTIFACT_TYPES, node.getArtifactTypes(), parameter))
                .print(printMap(YamlSpecKeywords.DATA_TYPES, node.getDataTypes(), parameter))
                .print(printMap(YamlSpecKeywords.CAPABILITY_TYPES, node.getCapabilityTypes(), parameter))
                .print(printMap(YamlSpecKeywords.INTERFACE_TYPES, node.getInterfaceTypes(), parameter))
                .print(printMap(YamlSpecKeywords.RELATIONSHIP_TYPES, node.getRelationshipTypes(), parameter))
                .print(printMap(YamlSpecKeywords.NODE_TYPES, node.getNodeTypes(), parameter))
                .print(printMap(YamlSpecKeywords.GROUP_TYPES, node.getGroupTypes(), parameter))
                .print(printMap(YamlSpecKeywords.POLICY_TYPES, node.getPolicyTypes(), parameter))
                .print(printVisitorNode(node.getTopologyTemplate(), new Parameter(parameter.getIndent()).addContext(YamlSpecKeywords.TOPOLOGY_TEMPLATE)));
    }

    public YamlPrinter visit(YTTopologyTemplateDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(printMap(YamlSpecKeywords.INPUTS, node.getInputs(), parameter))
                .print(printMap(YamlSpecKeywords.NODE_TEMPLATES, node.getNodeTemplates(), parameter))
                .print(printMap(YamlSpecKeywords.RELATIONSHIP_TEMPLATES, node.getRelationshipTemplates(), parameter))
                .print(printMap(YamlSpecKeywords.GROUPS, node.getGroups(), parameter))
                .print(printListMap(YamlSpecKeywords.POLICIES,
                        node.getPolicies().size() > 0 ? Collections.singletonList(node.getPolicies()) : new ArrayList<>(), parameter))
                .print(printMap(YamlSpecKeywords.OUTPUTS, node.getOutputs(), parameter))
                .print(printVisitorNode(node.getSubstitutionMappings(), new Parameter(parameter.getIndent()).addContext(YamlSpecKeywords.SUBSTITUTION_MAPPINGS)))
                .print(printMap(YamlSpecKeywords.WORKFLOWS, node.getWorkflows(), parameter));
    }

    public YamlPrinter visit(Metadata node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (!node.isEmpty()) {
            printer.printKey(YamlSpecKeywords.METADATA).indent(INDENT_SIZE);
            node.forEach((key, value) -> printer.printKeyValue(key, value, true));
            printer.indent(-INDENT_SIZE);
        }
        return printer;
    }

    public YamlPrinter visit(YTRepositoryDefinition node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printKeyValue(YamlSpecKeywords.URL, node.getUrl());
        if (Objects.nonNull(node.getCredential())) {
            Credential credential = node.getCredential();
            printer.printKey(YamlSpecKeywords.CREDENTIAL)
                    .indent(INDENT_SIZE)
                    .printKeyValue(YamlSpecKeywords.PROTOCOL, credential.getProtocol())
                    .printKeyValue(YamlSpecKeywords.TOKEN_TYPE, credential.getTokenType())
                    .printKeyValue(YamlSpecKeywords.TOKEN, credential.getToken())
                    .printKeyObject(YamlSpecKeywords.KEYS, credential.getKeys())
                    .printKeyValue(YamlSpecKeywords.USER, credential.getUser())
                    .indent(-INDENT_SIZE);
        }
        return printer;
    }

    public YamlPrinter visit(YTImportDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.FILE, node.getFile())
                .printKeyValue(YamlSpecKeywords.REPOSITORY, node.getRepository())
                .printKeyValue(YamlSpecKeywords.NAMESPACE_URI, node.getNamespaceUri())
                // .printKeyValue("namespace_uri", node.getNamespaceUri(), !node.getNamespaceUri().equals(Namespaces.DEFAULT_NS))
                .printKeyValue(YamlSpecKeywords.NAMESPACE_PREFIX, node.getNamespacePrefix());
    }

    public YamlPrinter visit(YTArtifactType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.MIME_TYPE, node.getMimeType())
                .printKeyValue(YamlSpecKeywords.FILE_EXT, node.getFileExt());
    }

    public YamlPrinter visit(YTEntityType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printKeyValue(YamlSpecKeywords.VERSION, node.getVersion())
                .printKeyValue(YamlSpecKeywords.DERIVED_FROM, node.getDerivedFrom())
                .print(node.getMetadata().accept(this, parameter))
                .print(printMap(YamlSpecKeywords.ATTRIBUTES, node.getAttributes(), parameter))
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter));
    }

    public YamlPrinter visit(YTPropertyDefinition node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription());
        // do not print the default value for required to avoid bloating the output
        if (!node.getRequired()) {
            printer.printKeyValue(YamlSpecKeywords.REQUIRED, node.getRequired());
        }
        printer.printYamlValue(YamlSpecKeywords.DEFAULT, node.getDefault());
        // do not print the default value for status to avoid bloating the output
        if (node.getStatus() != YTStatusValue.supported) {
            printer.printKeyValue(YamlSpecKeywords.STATUS, node.getStatus());
        }
        printer.print(printList(YamlSpecKeywords.CONSTRAINTS, node.getConstraints(), parameter));
        if (node.getEntrySchema() != null) {
            printer.printKey(YamlSpecKeywords.ENTRY_SCHEMA).print(visit(node.getEntrySchema(), new Parameter(parameter.getIndent() + INDENT_SIZE)));
        }
        if (node.getKeySchema() != null) {
            printer.printKey(YamlSpecKeywords.KEY_SCHEMA).print(visit(node.getKeySchema(), new Parameter(parameter.getIndent() + INDENT_SIZE)));
        }

        return printer;
    }

    public YamlPrinter visit(YTSchemaDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(printList(YamlSpecKeywords.CONSTRAINTS, node.getConstraints(), parameter));
    }

    public YamlPrinter visit(YTAttributeDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printYamlValue(YamlSpecKeywords.DEFAULT, node.getDefault())
                .printKeyValue(YamlSpecKeywords.STATUS, node.getStatus())
                .print(printVisitorNode(node.getEntrySchema(), parameter.addContext(YamlSpecKeywords.ENTRY_SCHEMA)));
    }

    public YamlPrinter visit(YTDataType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .print(printList(YamlSpecKeywords.CONSTRAINTS, node.getConstraints(), parameter));
    }

    public YamlPrinter visit(YTCapabilityType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.VALID_SOURCE_TYPES, node.getValidSourceTypes());
    }

    public YamlPrinter visit(YTInterfaceType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .print(printMap(YamlSpecKeywords.OPERATIONS, node.getOperations(), parameter))
                .print(printMap(YamlSpecKeywords.INPUTS, node.getInputs(), parameter));
    }

    public YamlPrinter visit(YTOperationDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(printMap(YamlSpecKeywords.INPUTS, node.getInputs(), new Parameter(parameter.getIndent())))
                .print(printMap(YamlSpecKeywords.OUTPUTS, node.getOutputs(), new Parameter(parameter.getIndent())))
                .print(printVisitorNode(node.getImplementation(), new Parameter(parameter.getIndent()).addContext(YamlSpecKeywords.IMPLEMENTATION)));
    }

    public YamlPrinter visit(YTWorkflow node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(printMap(YamlSpecKeywords.INPUTS, node.getInputs(), new Parameter(parameter.getIndent())))
                .print(printMap(YamlSpecKeywords.OUTPUTS, node.getOutputs(), new Parameter(parameter.getIndent())))
                .print(printVisitorNode(node.getImplementation(), new Parameter(parameter.getIndent()).addContext(YamlSpecKeywords.IMPLEMENTATION)));
    }

    public YamlPrinter visit(YTImplementation node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.PRIMARY, node.getPrimaryArtifactName())
                .printKeyValue(YamlSpecKeywords.DEPENDENCIES, node.getDependencyArtifactNames())
                .printKeyValue(YamlSpecKeywords.OPERATION_HOST, node.getOperationHost());
        if (node.getTimeout() != null) {
            printer.printKeyValue(YamlSpecKeywords.TIMEOUT, node.getTimeout().toString());
        }
        return printer;
    }

    public YamlPrinter visit(YTRelationshipType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.VALID_TARGET_TYPES, node.getValidTargetTypes())
                .print(printMap(YamlSpecKeywords.INTERFACES, node.getInterfaces(), parameter));
    }

    public YamlPrinter visit(YTInterfaceDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .print(printMap(YamlSpecKeywords.INPUTS, node.getInputs(), parameter))
                .print(printMap(YamlSpecKeywords.OPERATIONS, node.getOperations(), parameter));

//            .print(node.getOperations().entrySet().stream()
//                .filter(entry -> Objects.nonNull(entry) && Objects.nonNull(entry.getValue()))
//                .map(entry ->
//                    printVisitorNode(entry.getValue(), new Parameter(parameter.getIndent()).addContext(entry.getKey()))
//                )
//                .reduce(YamlPrinter::print)
//            );
    }

    public YamlPrinter visit(YTNodeType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .print(printListMap(YamlSpecKeywords.REQUIREMENTS, node.getRequirements().stream().map(YTMapRequirementDefinition::getMap).collect(Collectors.toList()), parameter))
                .print(printMap(YamlSpecKeywords.CAPABILITIES, node.getCapabilities(), parameter))
                .print(printMap(YamlSpecKeywords.INTERFACES, node.getInterfaces(), parameter))
                .print(printMap(YamlSpecKeywords.ARTIFACTS, node.getArtifacts(), parameter));
    }

    public YamlPrinter visit(YTRequirementDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.CAPABILITY, node.getCapability())
                .printKeyValue(YamlSpecKeywords.NODE, node.getNode())
                .print(printVisitorNode(node.getRelationship(), new Parameter(parameter.getIndent()).addContext(YamlSpecKeywords.RELATIONSHIP)))
                .printKeyValue(YamlSpecKeywords.OCCURRENCES, node.getOccurrences())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription());
    }

    public YamlPrinter visit(YTRelationshipDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                // Removed to support short notations
                // .printKeyValue("type", node.getType())
                .print(printMap(YamlSpecKeywords.INTERFACES, node.getInterfaces(), parameter));
    }

    public YamlPrinter visit(YTCapabilityDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printKeyValue(YamlSpecKeywords.OCCURRENCES, node.getOccurrences())
                .printKeyValue(YamlSpecKeywords.VALID_SOURCE_TYPES, node.getValidSourceTypes())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter))
                .print(printMap(YamlSpecKeywords.ATTRIBUTES, node.getAttributes(), parameter));
    }

    public YamlPrinter visit(YTArtifactDefinition node, Parameter parameter) {
        YamlPrinter output = new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.REPOSITORY, node.getRepository())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printKeyValue(YamlSpecKeywords.DEPLOY_PATH, node.getDeployPath());
        if (node.getFile() != null) {
            output.printKeyValue(YamlSpecKeywords.FILE, node.getFile());
        }
        return output;
    }

    public YamlPrinter visit(YTGroupType node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.MEMBERS, node.getMembers());
    }

    public YamlPrinter visit(YTPolicyType node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TARGETS, node.getTargets());

        Map<String, YTTriggerDefinition> validTriggers = node.getTriggers().entrySet()
                .stream()
                .filter(entry -> Objects.nonNull(entry.getValue().getEvent()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return printer.print(printMap(YamlSpecKeywords.TRIGGERS, validTriggers, parameter));
    }

    public YamlPrinter visit(YTTriggerDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printKeyValue(YamlSpecKeywords.EVENT, node.getEvent())
                .print(node.getTargetFilter().accept(this, parameter))
                .print(printListMap(YamlSpecKeywords.ACTION, node.getAction().stream().map(YTMapActivityDefinition::getMap).collect(Collectors.toList()), parameter));
    }

    public YamlPrinter visit(YTEventFilterDefinition node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (Objects.nonNull(node) && Objects.nonNull(node.getNode())) {
            printer.printKey(YamlSpecKeywords.TARGET_FILTER).indent(INDENT_SIZE);
            printer.printKeyValue(YamlSpecKeywords.NODE, node.getNode());
            if (Objects.nonNull(node.getRequirement())) {
                printer.printKeyValue(YamlSpecKeywords.REQUIREMENT, node.getRequirement());
            }
            if (Objects.nonNull(node.getRequirement())) {
                printer.printKeyValue(YamlSpecKeywords.CAPABILITY, node.getCapability());
            }
            printer.indent(-INDENT_SIZE);
        }
        return printer;
    }

    public YamlPrinter visit(YTCallOperationActivityDefinition node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.OPERATION, node.getOperation());
        if (Objects.nonNull(node.getInputs()) && !node.getInputs().isEmpty()) {
            printer.printKey(YamlSpecKeywords.INPUTS).indent(INDENT_SIZE);
            SortedSet<String> keys = new TreeSet<>(node.getInputs().keySet());
            for (String key : keys) {
                Object value = node.getInputs().get(key).getValue();
                if (value instanceof String) {
                    printer.printKeyValue(key, (String) value);
                }
            }
            printer.indent(-INDENT_SIZE);
        }
        return printer;
    }

    public YamlPrinter visit(YTVersion node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .print(node.getVersion());
    }

    public YamlPrinter visit(YTNodeTemplate node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(node.getMetadata().accept(this, parameter))
                .printKeyValue(YamlSpecKeywords.DIRECTIVES, node.getDirectives())
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter))
                .print(printMap(YamlSpecKeywords.ATTRIBUTES, node.getAttributes(), parameter))
                .print(printListMap(YamlSpecKeywords.REQUIREMENTS, node.getRequirements().stream().map(YTMapRequirementAssignment::getMap).collect(Collectors.toList()), parameter))
                .print(printMap(YamlSpecKeywords.CAPABILITIES, node.getCapabilities(), parameter))
                .print(printMap(YamlSpecKeywords.INTERFACES, node.getInterfaces(), parameter))
                .print(printMap(YamlSpecKeywords.ARTIFACTS, node.getArtifacts(), parameter))
                .print(printVisitorNode(node.getNodeFilter(), parameter))
                .printKeyValue(YamlSpecKeywords.COPY, node.getCopy());
    }

    public YamlPrinter visit(YTGroupDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(node.getMetadata().accept(this, parameter))
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter))
                .printKeyValue(YamlSpecKeywords.MEMBERS, node.getMembers());
    }

    public YamlPrinter visit(YTPolicyDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(node.getMetadata().accept(this, parameter))
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter))
                .printKeyValue(YamlSpecKeywords.TARGETS, node.getTargets());
    }

    public YamlPrinter visit(YTPropertyAssignment node, Parameter parameter) {
        // nested assignments are implemented by calling #printMap for Map values that are not property functions
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (node.getValue() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, YTPropertyAssignment> value = (Map<String, YTPropertyAssignment>) node.getValue();
            // special casing for property functions to always be a single-line map value
            if (value.size() == 1 && Arrays.stream(PROPERTY_FUNCTIONS).anyMatch(value::containsKey)) {
                String key = value.keySet().iterator().next();
                final Object rawFunctionArg = value.get(key).getValue();
                final String functionArg;
                if (rawFunctionArg instanceof List) {
                    final String list = ((List<?>) rawFunctionArg).stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                    // get_operation_output value is not in brackets, compare Section 4.6.1 of YAML-Standard 1.3
                    if (key.equals("get_operation_output")) {
                        functionArg = list;
                    } else {
                        functionArg = "[ " + list + " ]";
                    }
                } else if (rawFunctionArg instanceof YTPropertyAssignment) {
                    functionArg = visit((YTPropertyAssignment) rawFunctionArg, new Parameter(0)).toString();
                } else if (rawFunctionArg instanceof String) {
                    functionArg = (String) rawFunctionArg;
                } else {
                    // TODO
                    LOGGER.warn("Unexpected value type [{}] in property function definition", rawFunctionArg.getClass().getName());
                    functionArg = "";
                }
                printer.print(parameter.getKey()).print(":")
                        .print(" { ")
                        .print(key).print(": ").print(functionArg)
                        .print(" }").printNewLine();
            } else if (value.isEmpty()) {
                // printMap would skip an empty map
                printer.printKeyValue(parameter.getKey(), "{}");
            } else {
                printer.print(printMap(parameter.getKey(), value, parameter));
            }
        } else if (node.getValue() instanceof List) {
            if (((List<?>) node.getValue()).isEmpty()) {
                // printList would skip an empty list
                printer.printKeyValue(parameter.getKey(), "[]");
            } else {
                printer.print(printList(parameter.getKey(), (List<?>) node.getValue(), parameter));
            }
        } else {
            // printKeyObject skips null and empty values, which is a reasonable default
            // therefore we serialize null values ourselves here
            final String value = Objects.toString(node.getValue());
            final boolean isStringValue = node.getValue() instanceof String;
            printer.printKeyValue(parameter.getKey(), value, isStringValue, false);
        }
        return printer;
    }

    public YamlPrinter visit(YTAttributeAssignment node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printYamlValue(parameter.getKey(), node.getValue(), true);
    }

    public YamlPrinter visit(YTInterfaceAssignment node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent());
    }

    public YamlPrinter visit(YTParameterDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .printKeyValue(YamlSpecKeywords.REQUIRED, node.getRequired())
                .printYamlValue(YamlSpecKeywords.DEFAULT, node.getDefault())
                .printKeyValue(YamlSpecKeywords.STATUS, node.getStatus())
                .print(printList(YamlSpecKeywords.CONSTRAINTS, node.getConstraints(), parameter))
                .print(printVisitorNode(node.getEntrySchema(), parameter))
                .printYamlValue(YamlSpecKeywords.VALUE, node.getValue());
    }

    public YamlPrinter visit(YTConstraintClause node, Parameter parameter) {
        if (node.getValue() != null) {
            return new YamlPrinter(parameter.getIndent())
                    .printKeyValue(node.getKey(), node.getValue());
        } else if (node.getList() != null) {
            return new YamlPrinter(parameter.getIndent())
                    .printKeyListObjectInline(node.getKey(), new ArrayList<>(node.getList()));
        }
        return null;
    }

    public YamlPrinter visit(YTCapabilityAssignment node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter))
                .print(printMap(YamlSpecKeywords.ATTRIBUTES, node.getAttributes(), parameter));
    }

    public YamlPrinter visit(YTNodeFilterDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .print(printListMap(YamlSpecKeywords.PROPERTIES,
                        node.getProperties().stream().map(YTMapPropertyFilterDefinition::getMap).collect(Collectors.toList()),
                        parameter)
                )
                .print(printListMap(YamlSpecKeywords.CAPABILITIES,
                        node.getCapabilities().stream().map(YTMapObject::getMap).collect(Collectors.toList()),
                        parameter)
                );
    }

    public YamlPrinter visit(YTRelationshipTemplate node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.TYPE, node.getType())
                .printKeyValue(YamlSpecKeywords.DESCRIPTION, node.getDescription())
                .print(node.getMetadata().accept(this, parameter))
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter))
                .print(printMap(YamlSpecKeywords.ATTRIBUTES, node.getAttributes(), parameter))
                .print(printMap(YamlSpecKeywords.INTERFACES, node.getInterfaces(), parameter))
                .printKeyValue(YamlSpecKeywords.COPY, node.getCopy());
    }

    public YamlPrinter visit(YTSubstitutionMappings node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.NODE_TYPE, node.getNodeType())
                .print(printMapInlineStringList(YamlSpecKeywords.CAPABILITIES, node.getCapabilities(), parameter));
    }

    public YamlPrinter visit(YTRequirementAssignment node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .printKeyValue(YamlSpecKeywords.NODE, node.getNode())
                .print(printVisitorNode(node.getRelationship(), new Parameter(parameter.getIndent()).addContext(YamlSpecKeywords.RELATIONSHIP)))
                .printKeyValue(YamlSpecKeywords.CAPABILITY, node.getCapability())
                .print(printVisitorNode(node.getNodeFilter(), new Parameter(parameter.getIndent()).addContext(YamlSpecKeywords.NODE_FILTER)))
                .printKeyValue(YamlSpecKeywords.OCCURRENCES, node.getOccurrences());
    }

    public YamlPrinter visit(YTRelationshipAssignment node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                // Removed to support short notations
                // .printKeyValue("type", node.getType())
                .print(printMap(YamlSpecKeywords.PROPERTIES, node.getProperties(), parameter))
                .print(printMap(YamlSpecKeywords.INTERFACES, node.getInterfaces(), parameter));
    }

    public YamlPrinter visit(YTPropertyFilterDefinition node, Parameter parameter) {
        return new YamlPrinter(parameter.getIndent())
                .print(printList(YamlSpecKeywords.CONSTRAINTS, node.getConstraints(), parameter));
    }

    public YamlPrinter printVisitorNode(VisitorNode node, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (Objects.nonNull(node)) {
            if (node instanceof YTPropertyAssignment) {
                printer.print(node.accept(this,
                        new Parameter(parameter.getIndent()).addContext(parameter.getKey())
                ));
            } else if (node instanceof YTRelationshipAssignment) {
                printer.print(parameter.getKey() + ": ").printQName(((YTRelationshipAssignment) node).getType())
                        .print(node.accept(this, new Parameter(parameter.getIndent() + INDENT_SIZE)));
            } else if (node instanceof YTRelationshipDefinition) {
                printer.print(parameter.getKey() + ": ").printQName(((YTRelationshipDefinition) node).getType())
                        .print(node.accept(this, new Parameter(parameter.getIndent() + INDENT_SIZE)));
            } else {
                printer.printKey(parameter.getKey())
                        .print(node.accept(this, new Parameter(parameter.getIndent() + INDENT_SIZE)));
            }
        }
        return printer;
    }

    private <T> YamlPrinter printList(String keyValue, List<T> list, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (list != null && !list.isEmpty()) {
            printer.printKey(keyValue);
            printer.indent(INDENT_SIZE);
            list.stream()
                    // transform items to yaml-strings without indentation
                    // since indentation is done in printListObject
                    .map(entry -> ((VisitorNode) entry).accept(this, new Parameter(0)))
                    .reduce(printer, YamlPrinter::printListObject);
            printer.indent(-INDENT_SIZE);
        }
        return printer;
    }

    private <T extends VisitorNode> YamlPrinter printMap(String keyValue, Map<String, T> map, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (map == null || map.isEmpty()) {
            return printer;
        }
        if (!keyValue.isEmpty()) {
            printer.printKey(keyValue);
        }
        printer.print(map.entrySet().stream()
                .map((entry) -> {
                            YamlPrinter p = new YamlPrinter(parameter.getIndent() + INDENT_SIZE)
                                    .print(
                                            printVisitorNode(entry.getValue(),
                                                    new Parameter(parameter.getIndent() + INDENT_SIZE).addContext(entry.getKey())
                                            )
                                    );
                            return p;
                        }
                )
                .reduce(YamlPrinter::print)
        );
        return printer;
    }

    private <T> YamlPrinter printListMap(String keyValue, List<Map<String, T>> list, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (list != null && !list.isEmpty()) {
            printer.printKey(keyValue)
                    .print(list.stream()
                            .flatMap(map -> map.entrySet().stream())
                            .map(
                                    (entry) -> new YamlPrinter(parameter.getIndent() + INDENT_SIZE)
                                            .printListKey(entry.getKey())
                                            // FIXME replace <T> with <T extends VisitorNode> to enforce type at compile time
                                            //  this also necessitates adjusting the yaml-model to contain that type information
                                            .print(((VisitorNode) entry.getValue())
                                                    .accept(this,
                                                            new Parameter(parameter.getIndent() + 3 * INDENT_SIZE).addContext(entry.getKey())
                                                    ))
                            )
                            .reduce(YamlPrinter::print)
                    );
        }
        return printer;
    }

    private YamlPrinter printMapInlineStringList(String keyValue, Map<String, YTListString> map, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (!map.isEmpty()) {
            printer.printKey(keyValue)
                    .print(map.entrySet().stream()
                            .map(entry -> new YamlPrinter(parameter.getIndent() + 2)
                                    .printKeyValue(entry.getKey(), entry.getValue())
                            )
                            .reduce(YamlPrinter::print)
                    );
        }
        return printer;
    }

    private YamlPrinter printMapObject(String keyValue, Map<String, ? extends Object> map, Parameter parameter) {
        YamlPrinter printer = new YamlPrinter(parameter.getIndent());
        if (!map.isEmpty()) {
            printer.printCheckNewLine()
                    .printKey(keyValue)
                    .indent(INDENT_SIZE);
            map.forEach(printer::printKeyObject);
            printer.indent(-INDENT_SIZE);
        }
        return printer;
    }

    public static class Parameter extends AbstractParameter<Parameter> {
        private final int indent;

        public Parameter(int indent) {
            this.indent = indent;
        }

        public int getIndent() {
            return this.indent;
        }

        @Override
        public Parameter copy() {
            return new Parameter(this.indent).addContext(this.getContext());
        }

        @Override
        public Parameter self() {
            return this;
        }
    }
}
