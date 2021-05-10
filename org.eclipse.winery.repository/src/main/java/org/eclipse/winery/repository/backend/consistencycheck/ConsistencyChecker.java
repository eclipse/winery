/********************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.backend.consistencycheck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.winery.common.ToscaDocumentBuilderFactory;
import org.eclipse.winery.model.ids.IdUtil;
import org.eclipse.winery.model.ids.Namespace;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.ids.definitions.EntityTemplateId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.extensions.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.common.RepositoryFileReference;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.CsarExporter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ConsistencyChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyChecker.class);
    private static final String ARTEFACT_BE = "artefact";

    private final ConsistencyCheckerConfiguration configuration;

    private ConsistencyCheckerProgressListener progressListener;
    private final List<DefinitionsChildId> allDefinitionsChildIds;

    // Updated throughout the check
    private final ConsistencyErrorCollector errorCollector = new ConsistencyErrorCollector();

    public ConsistencyChecker(@NonNull ConsistencyCheckerConfiguration configuration) {
        this(configuration, new ConsistencyCheckerProgressListener() {
        });
    }

    public ConsistencyChecker(@NonNull ConsistencyCheckerConfiguration configuration,
                              @NonNull ConsistencyCheckerProgressListener consistencyCheckerProgressListener) {
        this.configuration = Objects.requireNonNull(configuration);
        this.progressListener = Objects.requireNonNull(consistencyCheckerProgressListener);
        if (configuration.getRepository() == null) {
            LOGGER.trace("Running in testing mode");
            this.allDefinitionsChildIds = Collections.emptyList();
        } else {
            LOGGER.trace("Running in normal mode");
            Set<DefinitionsChildId> allDefinitionsChildIds = configuration.getRepository().getAllDefinitionsChildIds();
            if (configuration.isServiceTemplatesOnly()) {
                allDefinitionsChildIds = allDefinitionsChildIds.stream().filter(id -> id instanceof ServiceTemplateId).collect(Collectors.toSet());
            }
            if (configuration.isTestMode()) {
                // we need a predictable ordering
                // in the current implementation, the set is sorted --> just convert it to a list
                this.allDefinitionsChildIds = new ArrayList<>(allDefinitionsChildIds);
            } else {
                // Random sorting of definitions ids to have the progressbar running at the same speed (and not being VERY slow at the end)
                this.allDefinitionsChildIds = allDefinitionsChildIds.stream().sorted(Comparator.comparingInt(DefinitionsChildId::hashCode)).collect(Collectors.toList());
            }
        }
    }

    public void setConsistencyCheckerProgressListener(ConsistencyCheckerProgressListener consistencyCheckerProgressListener) {
        this.progressListener = consistencyCheckerProgressListener;
    }

    public int numberOfDefinitionsToCheck() {
        return allDefinitionsChildIds.size();
    }

    /**
     * This method may be called only once during the lifecycle of this object
     */
    public void checkCorruption() {
        if (configuration.getVerbosity().contains(ConsistencyCheckerVerbosity.OUTPUT_NUMBER_OF_TOSCA_COMPONENTS)) {
            LOGGER.info("Number of TOSCA definitions to check: {}", numberOfDefinitionsToCheck());
        }

        checkAllDefinitions(configuration.getRepository());
    }

    /**
     * Checks whether a README.md and a LICENSE file exists for the given definitions child id.
     */
    private void checkDocumentation(DefinitionsChildId id) {
        checkFileExistenceAndSize(id, "README.md");
        checkFileExistenceAndSize(id, "LICENSE");
    }

    /**
     * Checks whether the given filename exists within the given defintions child id and if the size is above a
     * threshold (currently 100 bytes)
     */
    private void checkFileExistenceAndSize(DefinitionsChildId id, String filename) {
        RepositoryFileReference repositoryFileReference = new RepositoryFileReference(id, filename);
        if (!configuration.getRepository().exists(repositoryFileReference)) {
            printAndAddWarning(id, filename + " does not exist.");
            return;
        }
        long size;
        try {
            size = configuration.getRepository().getSize(repositoryFileReference);
        } catch (IOException e) {
            LOGGER.debug("Could not determine size for {}", id.toReadableString(), e);
            printAndAddError(id, "Could not determine size, because " + e);
            return;
        }
        if (size < 100) {
            printAndAddWarning(id, filename + " has size of less then 100 bytes.");
        }
    }

    private void checkPlainConformance(DefinitionsChildId id, Path tempCsar) {
        // TODO implement according to https://winery.github.io/test-repository/plain
        /*if (id.getNamespace().getDecoded().startsWith("http://plain.winery.opentosca.org/")) {
            if (id instanceof EntityTypeId) {
                if (id.getXmlId().getDecoded().endsWith("WithoutProperties")) {
                    // TODO
                }
            }
        }*/
    }

    private void checkServiceTemplate(ServiceTemplateId id) {
        TServiceTemplate serviceTemplate;
        try {
            serviceTemplate = configuration.getRepository().getElement(id);
        } catch (IllegalStateException e) {
            LOGGER.debug("Illegal State Exception during reading of id {}", id.toReadableString(), e);
            printAndAddError(id, "Reading error " + e.getMessage());
            return;
        }
        if (serviceTemplate.getTopologyTemplate() == null) {
            return;
        }
        @NonNull final List<TNodeTemplate> nodeTemplates = serviceTemplate.getTopologyTemplate().getNodeTemplates();
        for (TNodeTemplate nodeTemplate : nodeTemplates) {
            NodeTypeId nodeTypeId = new NodeTypeId(nodeTemplate.getType());
            TNodeType nodeType;
            try {
                nodeType = configuration.getRepository().getElement(nodeTypeId);
            } catch (IllegalStateException e) {
                LOGGER.debug("Illegal State Exception during reading of id {}", nodeTypeId.toReadableString(), e);
                printAndAddError(nodeTypeId, "Reading error " + e.getMessage());
                return;
            }
            final WinerysPropertiesDefinition winerysPropertiesDefinition = nodeType.getWinerysPropertiesDefinition();
            if (winerysPropertiesDefinition != null) {
                List<PropertyDefinitionKV> list = winerysPropertiesDefinition.getPropertyDefinitions();
                if (list != null) {
                    // iterate on all defined properties
                    for (PropertyDefinitionKV propdef : list) {
                        String key = propdef.getKey();
                        if (key == null) {
                            printAndAddError(id, "key is null");
                            continue;
                        }
                        // assign value, but change "null" to "" if no property is defined
                        final Map<String, String> propertiesKV = ModelUtilities.getPropertiesKV(nodeTemplate);
                        if (propertiesKV == null) {
                            printAndAddError(id, "propertiesKV of node template " + nodeTemplate.getId() + " is null");
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks all references QNames whether they are valid
     */
    private void checkReferencedQNames(DefinitionsChildId id, Map<QName, TExtensibleElements> allQNameToElementMapping) {
        final QNameValidator qNameValidator = new QNameValidator(error -> printAndAddError(id, error), allQNameToElementMapping);
        try {
            configuration.getRepository().getDefinitions(id).getElement()
                .accept(qNameValidator);
        } catch (IllegalStateException e) {
            LOGGER.debug("Illegal State Exception during reading of id {}", id.toReadableString(), e);
            printAndAddError(id, "Reading error " + e.getMessage());
        }
    }

    private void checkXmlSchemaValidation(DefinitionsChildId id) {
        RepositoryFileReference refOfDefinitions = BackendUtils.getRefOfDefinitions(id);
        if (!configuration.getRepository().exists(refOfDefinitions)) {
            printAndAddError(id, "Id exists, but corresponding XML file does not.");
            return;
        }
        try (InputStream inputStream = configuration.getRepository().newInputStream(refOfDefinitions)) {
            DocumentBuilder documentBuilder = ToscaDocumentBuilderFactory.INSTANCE.getSchemaAwareToscaDocumentBuilder();
            StringBuilder errorStringBuilder = new StringBuilder();
            documentBuilder.setErrorHandler(BackendUtils.getErrorHandler(errorStringBuilder));
            documentBuilder.parse(inputStream);
            String errors = errorStringBuilder.toString();
            if (!errors.isEmpty()) {
                printAndAddError(id, errors);
            }
        } catch (IOException e) {
            LOGGER.debug("I/O error", e);
            printAndAddError(id, "I/O error during XML validation " + e.getMessage());
        } catch (SAXException e) {
            LOGGER.debug("SAX exception", e);
            printAndAddError(id, "SAX error during XML validation: " + e.getMessage());
        }
    }

    private void validate(RepositoryFileReference xmlSchemaFileReference, @Nullable Object any, DefinitionsChildId id) {
        if (!(any instanceof Element)) {
            if (any == null) {
                printAndAddError(id, "any is not an Object!");
                return;
            }
            printAndAddError(id, "any is not instance of Document, but " + any.getClass());
            return;
        }
        Element element = (Element) any;
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream inputStream = configuration.getRepository().newInputStream(xmlSchemaFileReference)) {
            Source schemaFile = new StreamSource(inputStream);
            Schema schema = factory.newSchema(schemaFile);
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(element));
        } catch (Exception e) {
            printAndAddError(id, "Error during validating XML schema " + e.getMessage());
        }
    }

    private void checkPropertiesValidation(DefinitionsChildId id) {
        if (!(id instanceof EntityTemplateId)) {
            return;
        }
        TEntityTemplate entityTemplate;
        try {
            // TEntityTemplate is abstract. IRepository does not offer getElement for abstract ids
            // Therefore, we have to use the detour through getDefinitions
            entityTemplate = (TEntityTemplate) configuration.getRepository().getDefinitions(id).getElement();
        } catch (IllegalStateException e) {
            LOGGER.debug("Illegal State Exception during reading of id {}", id.toReadableString(), e);
            printAndAddError(id, "Reading error " + e.getMessage());
            return;
        } catch (ClassCastException e) {
            LOGGER.error("Something wrong in the consistency between Ids and the TOSCA data model. See http://eclipse.github.io/winery/dev/id-system.html for more information on the ID system.");
            printAndAddError(id, "Critical error at analysis: " + e.getMessage());
            return;
        }
        if (Objects.isNull(entityTemplate.getType())) {
            // no printing necessary; type consistency is checked at other places
            return;
        }

        TEntityType entityType;
        try {
            entityType = configuration.getRepository().getTypeForTemplate(entityTemplate);
        } catch (IllegalStateException e) {
            LOGGER.debug("Illegal State Exception during getting type for template {}", entityTemplate.getId(), e);
            printAndAddError(id, "Reading error " + e.getMessage());
            return;
        }
        TEntityTemplate.Properties definedProps = entityTemplate.getProperties();
        if (requiresProperties(entityType) && definedProps == null) {
            printAndAddError(id, "Properties required, but no properties defined");
            return;
        } else if (!requiresProperties(entityType) && definedProps != null) {
            printAndAddError(id, "No properties required by type, but properties were defined on template");
            return;
        } else if (definedProps == null) {
            // no properties required and none defined
            return;
        }

        if (definedProps instanceof TEntityTemplate.XmlProperties) {
            // check defined properties any against the xml schema 
            @Nullable final Object any = ((TEntityTemplate.XmlProperties) definedProps).getAny();
            if (any == null) {
                printAndAddError(id, "Properties required, but no XmlProperties were empty (any case)");
                return;
            }

            TEntityType.PropertiesDefinition def = entityType.getProperties();
            if (def == null) {
                printAndAddError(id, "XmlProperties were given, but no XmlPropertiesDefinition was specified");
                return;
            }
            if (def instanceof TEntityType.XmlElementDefinition) {
                final QName element = ((TEntityType.XmlElementDefinition) def).getElement();
                final Map<String, RepositoryFileReference> mapFromLocalNameToXSD = configuration.getRepository().getXsdImportManager().getMapFromLocalNameToXSD(new Namespace(element.getNamespaceURI(), false), false);
                final RepositoryFileReference repositoryFileReference = mapFromLocalNameToXSD.get(element.getLocalPart());
                if (repositoryFileReference == null) {
                    printAndAddError(id, "No Xml Schema definition found for " + element);
                    return;
                }
                validate(repositoryFileReference, any, id);
            }
        } else if (definedProps instanceof TEntityTemplate.WineryKVProperties) {
            final WinerysPropertiesDefinition winerysPropertiesDefinition = entityType.getWinerysPropertiesDefinition();
            Map<String, String> kvProperties = ((TEntityTemplate.WineryKVProperties) definedProps).getKVProperties();
            if (kvProperties.isEmpty()) {
                printAndAddError(id, "Properties required, but no properties set (kvproperties case)");
                return;
            }
            for (PropertyDefinitionKV propertyDefinitionKV : winerysPropertiesDefinition.getPropertyDefinitions()) {
                String key = propertyDefinitionKV.getKey();
                if (kvProperties.get(key) == null) {
                    printAndAddError(id, "Property " + key + " required, but not set.");
                } else {
                    // removeNamespaceProperties the key from the map to enable checking below whether a property is defined which not requried by the property definition 
                    kvProperties.remove(key);
                }
            }
            // All winery-property-definition-keys have been removed from kvProperties.
            // If any key is left, this is a key not defined at the schema
            for (Object o : kvProperties.keySet()) {
                printAndAddError(id, "Property " + o + " set, but not defined at schema.");
            }
        } else if (definedProps instanceof TEntityTemplate.YamlProperties) {
            // FIXME todo
            LOGGER.debug("YAML Properties checking is not yet implemented!");
        }
    }

    private static boolean requiresProperties(TEntityType type) {
        return type.getProperties() != null;
    }

    private void checkId(DefinitionsChildId id) {
        checkNamespaceUri(id);
        checkNcname(id, id.getXmlId().getDecoded());
    }

    private void checkNcname(DefinitionsChildId id, String ncname) {
        if (!ncname.trim().equals(ncname)) {
            printAndAddError(id, "local name starts or ends with white spaces");
        }
        if (ncname.toLowerCase().contains(ARTEFACT_BE)) {
            printAndAddError(id, "artifact is spelled with i in American English, not artefact as in British English");
        }
    }

    public void checkNamespaceUri(@NonNull DefinitionsChildId id) {
        Objects.requireNonNull(id);

        String uriStr = id.getNamespace().getDecoded();
        if (!uriStr.trim().equals(uriStr)) {
            printAndAddError(id, "Namespace starts or ends with white spaces");
        }
        URI uri;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException e) {
            LOGGER.debug("Invalid URI", e);
            printAndAddError(id, "Invalid URI: " + e.getMessage());
            return;
        }
        if (!uri.isAbsolute()) {
            printAndAddError(id, "URI is relative");
        }
        if ((uriStr.startsWith("http://www.opentosca.org/") && (!uriStr.toLowerCase().equals(uriStr)))) {
            // URI is not lowercase
            // There are some special URIs, which are OK
            String[] splitUri = uriStr.split("/");
            String lastElement = splitUri[splitUri.length - 1];
            String uriStrWithoutLastElement = uriStr.substring(0, (uriStr.length() - lastElement.length()));
            if (!(id.getXmlId().toString().startsWith(lastElement)) || (!uriStrWithoutLastElement.toLowerCase().equals(uriStrWithoutLastElement))) {
                printAndAddError(id, "opentosca URI is not lowercase");
            }
        }
        if (uriStr.endsWith("/")) {
            printAndAddError(id, "URI ends with a slash");
        }
        if (uriStr.contains(ARTEFACT_BE)) {
            printAndAddError(id, "artifact is spelled with i in American English, not artefact as in British English");
        }
        // We could just check OpenTOSCA namespace rule examples. However, this would be too strict
        // Here, the idea is to check whether a string of another (!) id class appers in the namespace
        // If this is the case, the namespace is not consistent
        // For instance, a node type residing in the namespace: http://servicetemplates.example.org should not exist.
        boolean namespaceUriContainsDifferentType = DefinitionsChildId.ALL_TOSCA_COMPONENT_ID_CLASSES.stream()
            .filter(definitionsChildIdClass -> !definitionsChildIdClass.isAssignableFrom(id.getClass()))
            // we have the issue that nodetypeimplementation also contains nodetype
            // we do the quick hack and check for plural s and /
            .flatMap(definitionsChildIdClass -> {
                final String lowerCaseIdClass = IdUtil.getTypeForComponentId(definitionsChildIdClass).toLowerCase();
                return Stream.of(lowerCaseIdClass + "s", lowerCaseIdClass + "/");
            })
            .anyMatch(uriStr::contains);
        if (namespaceUriContainsDifferentType) {
            if ((id instanceof ServiceTemplateId) && (id.getNamespace().getDecoded().contains("compliance"))) {
                // special case, because TComplianceRule models a service template, but Compliance Rules are treated as Service Template during modeling
                // example: class org.eclipse.winery.common.ids.definitions.ServiceTemplateId / {http://www.compliance.opentosca.org/compliancerules}Satisfied_Compliance_Rule_Example_w1
                printAndAddWarning(id, "Cannot perform checks for compliance rules...");
            } else {
                printAndAddError(id, "Namespace URI contains tosca definitions name from other type. E.g., Namespace is ...servicetemplates..., but the type is an artifact template");
            }
        }
    }

    private void checkCsar(DefinitionsChildId id, Path tempCsar, IRepository repository) {
        CsarExporter exporter = new CsarExporter(repository);
        Map<String, Object> exportConfiguration = new HashMap<>();
        try (OutputStream outputStream = Files.newOutputStream(tempCsar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            try {
                exporter.writeCsar(id, outputStream, exportConfiguration);
            } catch (IOException e) {
                LOGGER.debug("Error during checking ZIP", e);
                printAndAddError(id, "I/O error: " + e.getMessage());
                return;
            } catch (RepositoryCorruptException e) {
                LOGGER.debug("Repository is corrupt", e);
                printAndAddError(id, "Corrupt: " + e.getMessage());
                return;
            } catch (Exception e) {
                LOGGER.debug("Inner error at writing to temporary CSAR file", e);
                printAndAddError(id, e.toString());
                return;
            }
        } catch (Exception e) {
            final String error = "Could not write to temp CSAR file";
            LOGGER.debug(error, e);
            printAndAddError(id, error);
            return;
        }

        try (InputStream inputStream = Files.newInputStream(tempCsar);
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (configuration.getVerbosity().contains(ConsistencyCheckerVerbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID)) {
                    LOGGER.info("Identified item in CSAR: {}", entry.getName());
                }
            }
        } catch (Exception e) {
            final String error = "Could not read from temp CSAR file";
            LOGGER.debug(error, e);
            printAndAddError(id, error);
        }
    }

    private void checkAllDefinitions(IRepository repository) {
        final Path tempCsar;

        try {
            tempCsar = Files.createTempFile("Export", ".csar");
        } catch (IOException e) {
            LOGGER.debug("Could not create temp CSAR file", e);
            errorCollector.error("Could not create temp CSAR file");
            return;
        }

        float elementsChecked = 0;
        int size = allDefinitionsChildIds.size();
        Map<QName, TExtensibleElements> allQNameToElementMapping = repository.getAllQNameToElementMapping();
        for (DefinitionsChildId id : allDefinitionsChildIds) {
            float progress = ++elementsChecked / size;
            if (configuration.getVerbosity().contains(ConsistencyCheckerVerbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID)) {
                progressListener.updateProgress(progress, id.toReadableString());
            } else {
                progressListener.updateProgress(progress);
            }

            checkId(id);
            checkXmlSchemaValidation(id);
            checkReferencedQNames(id, allQNameToElementMapping);
            checkPropertiesValidation(id);
            if (id instanceof ServiceTemplateId) {
                checkServiceTemplate((ServiceTemplateId) id);
            }
            if (configuration.isCheckDocumentation()) {
                checkDocumentation(id);
            }
            checkPlainConformance(id, tempCsar);
            checkCsar(id, tempCsar, repository);
        }
    }

    private void printAndAddError(DefinitionsChildId id, String error) {
        printError(id, error);
        errorCollector.error(id, error);
    }

    private void printAndAddWarning(DefinitionsChildId id, String error) {
        printError(id, error);
        errorCollector.warning(id, error);
    }

    private void printError(DefinitionsChildId id, String error) {
        if (configuration.getVerbosity().contains(ConsistencyCheckerVerbosity.OUTPUT_ERRORS)) {
            if (configuration.getVerbosity().contains(ConsistencyCheckerVerbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID)) {
                LOGGER.info("Currently handling {}, errors: {}", id, error);
            } else {
                LOGGER.info("Found Error: {}", error);
            }
        }
    }

    public ConsistencyErrorCollector getErrorCollector() {
        return this.errorCollector;
    }
}
