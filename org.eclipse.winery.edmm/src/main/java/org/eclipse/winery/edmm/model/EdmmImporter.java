/*******************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.edmm.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.eclipse.winery.edmm.EdmmManager;
import org.eclipse.winery.edmm.EdmmUtils;
import org.eclipse.winery.edmm.utils.ZipUtility;
import org.eclipse.winery.model.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.NodeTypeImplementationId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeImplementationId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import io.github.edmm.core.parser.Entity;
import io.github.edmm.core.parser.EntityGraph;
import io.github.edmm.core.parser.ScalarEntity;
import io.github.edmm.core.parser.support.DefaultKeys;
import io.github.edmm.model.DeploymentModel;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class EdmmImporter {

    private final static Logger logger = LoggerFactory.getLogger(EdmmImporter.class);

    private final Map<QName, TNodeType> nodeTypes;
    private final Map<QName, TNodeTypeImplementation> nodeTypeImplementations;
    private final Map<String, Map.Entry<QName, TNodeType>> normalizedNodeTypes = new HashMap<>();

    private final Map<QName, TRelationshipType> relationshipTypes;

    private final Map<String, Map.Entry<QName, TRelationshipType>> normalizedRelationshipTypes = new HashMap<>();
    private final Map<QName, TRelationshipTypeImplementation> relationshipTypeImplementations;

    private final Map<QName, TArtifactTemplate> artifactTemplates;
    private final Map<String, Map.Entry<QName, TArtifactTemplate>> normalizedArtifactTemplates = new HashMap<>();

    private final Map<EdmmType, QName> edmmToToscaMap;

    private final IRepository repository;

    private final String nodeTypesString = "nodeTypes";
    private final String relationshipTypesString = "relationshipTypes";

    public EdmmImporter() {
        logger.debug("Initializing EDMM Importer...");

        this.repository = RepositoryFactory.getRepository();

        this.nodeTypes = repository.getQNameToElementMapping(NodeTypeId.class);
        this.relationshipTypes = repository.getQNameToElementMapping(RelationshipTypeId.class);
        this.nodeTypeImplementations = repository.getQNameToElementMapping(NodeTypeImplementationId.class);
        this.relationshipTypeImplementations = repository.getQNameToElementMapping(RelationshipTypeImplementationId.class);
        this.artifactTemplates = repository.getQNameToElementMapping(ArtifactTemplateId.class);

        normalizeToscaTypes();

        EdmmManager edmmManager = EdmmManager.forRepository(repository);
        this.edmmToToscaMap = edmmManager.getEdmmOneToToscaMap();

        logger.info("Initialized EDMM Importer!");
    }

    public void importFromStream(InputStream uploadedInputStream, String fileName, boolean overwrite) {
        Path tempFile = null;
        try {
            tempFile = FileUtils.getTempDirectory().toPath().resolve(fileName);
            Files.copy(uploadedInputStream, tempFile, REPLACE_EXISTING);

            transform(tempFile, overwrite);
        } catch (IOException e) {
            logger.error("Cloud not save uploaded file!");
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null) {
                try {
                    FileUtils.forceDelete(tempFile.toFile());
                } catch (IOException e) {
                    logger.warn("Could not delete temp file!");
                }
            }
        }
    }

    public boolean transform(Path edmmFilePath, boolean overwrite) {
        String pathString = edmmFilePath.toString();
        logger.info("Received path \"{}\" to import.", pathString);

        Path workingDirectory = edmmFilePath;
        Path edmmEntryFilePath = null;

        if (pathString.endsWith(".zip")) {
            try {
                workingDirectory = ZipUtility.unpack(
                    edmmFilePath,
                    Files.createTempDirectory(edmmFilePath.getFileName().toString()).toAbsolutePath()
                );
            } catch (IOException e) {
                logger.error("Could not create temporary directory!", e);
                return false;
            }
        } else if (pathString.endsWith(".yml") || pathString.endsWith(".yaml")) {
            workingDirectory = edmmFilePath.getParent();
            edmmEntryFilePath = edmmFilePath;
        }

        if (edmmEntryFilePath == null) {
            Path rootDir = workingDirectory;
            File[] files = workingDirectory.toFile()
                .listFiles((dir, name) ->
                    dir.equals(rootDir.toFile()) && (name.endsWith(".yml") || name.endsWith(".yaml"))
                );

            if (files == null || files.length == 0) {
                logger.error("Could not find EDMM file!");
                return false;
            }

            edmmEntryFilePath = files[0].toPath();
        }

        try {
            // If the EDMM Model does not contain components, the DeploymentModel.of throws an IllegalStateException
            DeploymentModel deploymentModel = DeploymentModel.of(edmmEntryFilePath.toFile());
            logger.info("Successfully stored EDMM deployment model\"{}\"", deploymentModel.getName());
            return importEdmmModel(deploymentModel, overwrite);
        } catch (Exception e) {
            logger.error("Error while loading EDMM model!", e);
            return false;
        }
    }

    public boolean transform(String edmmYaml, boolean overwrite) {
        return importEdmmModel(DeploymentModel.of(edmmYaml), overwrite);
    }

    private boolean importEdmmModel(DeploymentModel deploymentModel, boolean overwrite) {
        logger.info("Starting to import \"{}\"", deploymentModel.getName());

        EntityGraph deploymentModelGraph = deploymentModel.getGraph();
        Optional<Entity> componentTypesOptional = deploymentModelGraph.getEntity(EntityGraph.COMPONENT_TYPES);
        if (componentTypesOptional.isPresent()) {
            Set<Entity> componentTypes = componentTypesOptional.get().getChildren();
            logger.debug("Found {} Component Types to import", componentTypes.size());

            componentTypes.forEach(this::importComponentTypes);
        }

        Optional<Entity> relationTypesOptional = deploymentModelGraph.getEntity(EntityGraph.RELATION_TYPES);
        if (relationTypesOptional.isPresent()) {
            Set<Entity> relationTypes = relationTypesOptional.get().getChildren();
            logger.debug("Found {} Relation Types to import", relationTypes.size());

            relationTypes.forEach(this::importRelationTypes);
        }

        Set<RootComponent> components = deploymentModel.getComponents();
        logger.debug("Found {} Components to import", components.size());
        importEdmmApplication(deploymentModel, components, overwrite);

        return true;
    }

    private void importRelationTypes(Entity entity) {
        String typeName = entity.getName();

        QName qName = existsQNameForType(typeName, relationshipTypesString);

        if (qName == null) {
            logger.debug("Creating new Relationship Type \"{}\"", typeName);
            qName = EdmmUtils.getQNameFromType(typeName, relationshipTypesString);
            TRelationshipType.Builder builder = new TRelationshipType.Builder(qName);

            importTypeSpecificElements(entity, builder, relationshipTypesString);

            RelationshipTypeId relationshipTypeId = new RelationshipTypeId(qName);
            try {
                repository.setElement(relationshipTypeId, builder.build());
            } catch (IOException e) {
                logger.error("Could not create Relationship Type with QName: \"{}\"", relationshipTypeId.getQName());
            }

            logger.debug("Created new Relationship Type \"{}\"", qName);
        }
    }

    private void importEdmmApplication(DeploymentModel deploymentModel, Set<RootComponent> components, boolean overwrite) {
        String deploymentModelName = deploymentModel.getName() != null
            ? deploymentModel.getName().replaceAll(".y(a?)ml", "")
            : "Imported-EDMM_" + System.currentTimeMillis();

        ServiceTemplateId serviceTemplateId = new ServiceTemplateId(
            new QName(EdmmUtils.IMPORTED_EDMM_NAMESPACE + "serviceTemplates", deploymentModelName)
        );
        if (repository.exists(serviceTemplateId) && !overwrite) {
            logger.info("Service Template with id \"{}\" already exists and should not be overridden!", serviceTemplateId.getQName());
            return;
        }

        logger.info("Importing EDMM-Model as Service Template with id \"{}\"", serviceTemplateId.getQName());

        TTopologyTemplate topologyTemplate = new TTopologyTemplate.Builder().build();
        components.forEach(entity -> importComponents(entity, topologyTemplate));
        components.forEach(entity -> importRelations(entity, topologyTemplate));

        TServiceTemplate importedServiceTemplate = new TServiceTemplate.Builder(
            deploymentModelName,
            serviceTemplateId.getNamespace().getDecoded(),
            topologyTemplate
        )
            .setName(deploymentModelName)
            .build();

        try {
            repository.setElement(
                serviceTemplateId,
                importedServiceTemplate
            );
        } catch (IOException e) {
            logger.error("Error while saving the ServiceTemplate \"{}\"", serviceTemplateId);
        }
    }

    private void importComponents(RootComponent component, TTopologyTemplate topologyTemplate) {
        QName type = getQNameForType(component.getType(), nodeTypesString);
        TNodeTemplate.Builder nodeBuilder = new TNodeTemplate.Builder(
            component.getId(),
            type
        )
            .setName(component.getName());

        addPropertiesToTEntityTemplate(component.getProperties(), nodeBuilder, nodeTypesString);

        topologyTemplate.addNodeTemplate(nodeBuilder.build());
    }

    private void importRelations(RootComponent entity, TTopologyTemplate topologyTemplate) {
        TNodeTemplate source = topologyTemplate.getNodeTemplate(entity.getId());

        entity.getRelations().forEach(relation -> {
            QName relationType = this.getQNameForType(relation.getId(), relationshipTypesString);

            TNodeTemplate target = topologyTemplate.getNodeTemplate(relation.getTarget());
            TRelationshipTemplate.Builder relationshipBuilder = new TRelationshipTemplate.Builder(
                entity.getId() + "-" + relation.getId() + "-" + relation.getTarget(),
                relationType,
                source,
                target
            ).setName(relation.getName());

            // Properties for relations are currently unsupported by the EDMM parser...
            // java.lang.ClassCastException: class io.github.edmm.core.parser.ScalarEntity cannot be cast to
            // class io.github.edmm.core.parser.MappingEntity (io.github.edmm.core.parser.ScalarEntity and
            // io.github.edmm.core.parser.MappingEntity are in unnamed module of loader 'app')
            // this.addPropertiesToTEntityTemplate(relation.getProperties(), relationshipBuilder, relationshipTypesString);

            topologyTemplate.addRelationshipTemplate(
                relationshipBuilder.build()
            );
        });
    }

    private void importComponentTypes(Entity entity) {
        String typeName = entity.getName();

        QName qName = existsQNameForType(typeName, nodeTypesString);

        if (qName == null) {
            logger.debug("Creating new Node Type \"{}\"", typeName);
            qName = EdmmUtils.getQNameFromType(typeName, nodeTypesString);
            TNodeType.Builder builder = new TNodeType.Builder(qName);

            importTypeSpecificElements(entity, builder, nodeTypesString);

            NodeTypeId nodeTypeId = new NodeTypeId(qName);
            try {
                repository.setElement(nodeTypeId, builder.build());
            } catch (IOException e) {
                logger.error("Could not create NodeType with QName: \"{}\"", nodeTypeId.getQName());
            }

            logger.debug("Created new Node Type \"{}\"", qName);
        }
    }

    private void importTypeSpecificElements(Entity entity, TEntityType.Builder<?> builder, String toscaType) {
        entity.getChildren().forEach(typeAttributes -> {
            if (typeAttributes.getName().equals(DefaultKeys.EXTENDS) && typeAttributes instanceof ScalarEntity) {
                String parentType = ((ScalarEntity) typeAttributes).getValue();
                if (parentType != null && !parentType.isEmpty()) {
                    QName parent = getQNameForType(parentType, toscaType);
                    builder.setDerivedFrom(parent);
                }
            }
            if (typeAttributes.getName().equals(DefaultKeys.PROPERTIES)) {
                addPropertiesToTEntityType(typeAttributes.getChildren(), builder);
            }
        });
    }

    private QName getQNameForType(String typeName, String toscaType) {
        QName qName = existsQNameForType(typeName, toscaType);

        if (qName == null) {
            logger.debug("Creating new Node Type \"{}\"", typeName);
            qName = EdmmUtils.getQNameFromType(typeName, toscaType);
        } else {
            logger.debug("Found existing Node Type \"{}\" matching requested Type! Reusing it...", qName);
            logger.debug("Type was: \"{}\"", typeName);
        }

        return qName;
    }

    private QName existsQNameForType(String typeName, String toscaType) {
        QName qName = this.edmmToToscaMap.get(new EdmmType(typeName));
        TEntityType type = null;

        if (qName != null) {
            if (toscaType.equals(nodeTypesString)) {
                type = this.nodeTypes.get(qName);
            } else if (toscaType.equals(relationshipTypesString)) {
                type = this.relationshipTypes.get(qName);
            }
        }

        if (type == null) {
            Map.Entry<QName, ?> entry = null;

            if (toscaType.equals(nodeTypesString)) {
                entry = normalizedNodeTypes.get(typeName);
            } else if (toscaType.equals(relationshipTypesString)) {
                entry = normalizedRelationshipTypes.get(typeName);
            }

            if (entry != null) {
                qName = entry.getKey();
            }
        }
        return qName;
    }

    private void addPropertiesToTEntityTemplate(Map<String, Property> propertiesMap, TEntityTemplate.Builder<?> tempalteBuilder, String toscaType) {
        TEntityTemplate.WineryKVProperties wineryKVProperties = new TEntityTemplate.WineryKVProperties();
        wineryKVProperties.setNamespace(EdmmUtils.IMPORTED_EDMM_NAMESPACE + toscaType);
        wineryKVProperties.setElementName("Properties");

        propertiesMap.forEach(
            (key, value) -> {
                if (!key.equals("name")) {
                    wineryKVProperties.addProperty(key, value.getValue());
                }
            }
        );

        if (wineryKVProperties.getKVProperties().size() > 0) {
            tempalteBuilder.setProperties(wineryKVProperties);
        }
    }

    private void addPropertiesToTEntityType(Set<Entity> children, TEntityType.Builder<?> builder) {
        ArrayList<PropertyDefinitionKV> propertyDefinitions = new ArrayList<>();
        children.forEach(property -> {
            String propertyName = property.getName();
            Optional<Entity> child = property.getChild(DefaultKeys.TYPE);
            if (child.isPresent()) {
                Entity entity = child.get();
                if (entity instanceof ScalarEntity) {
                    propertyDefinitions.add(
                        new PropertyDefinitionKV(propertyName, ((ScalarEntity) entity).getValue())
                    );
                }
            } else {
                logger.warn("Could not find property type of Property \"{}\"", property);
            }
        });

        if (!propertyDefinitions.isEmpty()) {
            TEntityType.PropertiesDefinition properties = builder.getProperties();
            if (properties instanceof WinerysPropertiesDefinition) {
                ((WinerysPropertiesDefinition) properties).getPropertyDefinitions().addAll(propertyDefinitions);
            } else {
                WinerysPropertiesDefinition winerysPropertiesDefinition = new WinerysPropertiesDefinition();
                winerysPropertiesDefinition.setPropertyDefinitions(propertyDefinitions);
                builder.setProperties(winerysPropertiesDefinition);
            }
        }
    }

    /**
     * Loads the required Type Definitions and creates a "normalized" definitions map.
     */
    private void normalizeToscaTypes() {
        nodeTypes.entrySet()
            .forEach(entry -> normalizedNodeTypes.put(
                EdmmUtils.normalizeQName(entry.getKey()),
                entry
            ));

        relationshipTypes.entrySet()
            .forEach(entry -> normalizedRelationshipTypes.put(
                EdmmUtils.normalizeQName(entry.getKey()),
                entry
            ));

        artifactTemplates.entrySet()
            .forEach(entry -> normalizedArtifactTemplates.put(
                EdmmUtils.normalizeQName(entry.getKey()),
                entry
            ));
    }
}
