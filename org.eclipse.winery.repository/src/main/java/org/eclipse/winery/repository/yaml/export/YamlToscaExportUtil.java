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

package org.eclipse.winery.repository.yaml.export;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.Constants;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TArtifact;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.common.RepositoryFileReference;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.CsarContentProperties;
import org.eclipse.winery.repository.export.ToscaExportUtil;
import org.eclipse.winery.repository.export.entries.RemoteRefBasedCsarEntry;
import org.eclipse.winery.repository.yaml.export.entries.YAMLDefinitionsBasedCsarEntry;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlToscaExportUtil extends ToscaExportUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlToscaExportUtil.class);
    private final boolean exportNormativeTypes;

    public YamlToscaExportUtil(boolean exportNormativeTypes) {
        this.exportNormativeTypes = exportNormativeTypes;
    }

    @Override
    protected Collection<DefinitionsChildId> processDefinitionsElement(IRepository repository, DefinitionsChildId tcId, CsarContentProperties definitionsFileProperties)
        throws RepositoryCorruptException, IOException {
        if (!repository.exists(tcId)) {
            String error = "Component instance " + tcId.toReadableString() + " does not exist.";
            LOGGER.error(error);
            throw new RepositoryCorruptException(error);
        }

        TDefinitions entryDefinitions = repository.getDefinitions(tcId);
        this.getPrepareForExport(repository, tcId, entryDefinitions);

        Collection<DefinitionsChildId> referencedDefinitionsChildIds = repository.getReferencedDefinitionsChildIds(tcId);

        if (!exportNormativeTypes) {
            referencedDefinitionsChildIds.removeIf(id -> id.getQName().getNamespaceURI().startsWith("tosca"));
        }

        // adjust imports: add imports of definitions
        Collection<TImport> imports = new ArrayList<>();
        Map<String, QName> importDefinitions = new HashMap<>();
        for (DefinitionsChildId id : referencedDefinitionsChildIds) {
            this.addToImports(repository, id, imports);

            String defName = YamlExporter.getDefinitionsName(repository, id).concat(Constants.SUFFIX_TOSCA_DEFINITIONS);
            importDefinitions.put(defName, id.getQName());
        }

        entryDefinitions.getImport().addAll(imports);
        entryDefinitions.setImportDefinitions(importDefinitions);

        // END: Definitions modification

        YAMLDefinitionsBasedCsarEntry entry = new YAMLDefinitionsBasedCsarEntry(repository, entryDefinitions);

        // Custom Adjustments for Service Templates
        YamlExportAdjustmentsBuilder adjustmentsBuilder = new YamlExportAdjustmentsBuilder(entry);
        if (!exportNormativeTypes) {
            adjustmentsBuilder.removeNormativeTypeImports();
        }
        entry = adjustmentsBuilder.setMetadataName(tcId).build();

        this.referencesToPathInCSARMap.put(definitionsFileProperties, entry);

        return referencedDefinitionsChildIds;
    }

    /**
     * Prepares the given id for export. Mostly, the contained files are added to the CSAR.
     */
    private void getPrepareForExport(IRepository repository, DefinitionsChildId id, TDefinitions entryDefinitions) throws IOException {
        if (id instanceof ServiceTemplateId) {
            this.prepareServiceTemplateForExport(repository, (ServiceTemplateId) id, entryDefinitions);
        } else if (id instanceof RelationshipTypeId) {
            this.addVisualAppearanceToCSAR(repository, (RelationshipTypeId) id);
            this.prepareRelationshipTypeForExport(repository, (RelationshipTypeId) id, entryDefinitions);
        } else if (id instanceof NodeTypeId) {
            this.addVisualAppearanceToCSAR(repository, (NodeTypeId) id);
            this.prepareNodeTypeForExport(repository, (NodeTypeId) id, entryDefinitions);
        }
    }

    private void prepareRelationshipTypeForExport(IRepository repository, RelationshipTypeId id, TDefinitions entryDefinitions) throws IOException {
        TRelationshipType node = repository.getElement(id);

        // convert locations of files in dependencies
        Path p = Paths.get("files");
        entryDefinitions.getRelationshipTypes()
            .stream()
            .filter(nt -> nt.getQName().equals(node.getQName()))
            .forEach(nt -> {
                if (nt.getInterfaceDefinitions() != null) {
                    nt.getInterfaceDefinitions().forEach(interfaceDefinition -> {
                        if (interfaceDefinition.getOperations() != null) {
                            interfaceDefinition.getOperations().forEach(op -> {
                                if (op.getImplementation() != null && op.getImplementation().getDependencies() != null) {
                                    List<String> dependencies = op.getImplementation().getDependencies().stream().map(artifactName -> {
                                        RepositoryFileReference ref = new RepositoryFileReference(id, p, artifactName);
                                        putRefAsReferencedItemInCsar(repository, ref);
                                        String pathInsideRepo = BackendUtils.getPathInsideRepo(ref);
                                        return "/" + FilenameUtils.separatorsToUnix(pathInsideRepo);
                                    }).collect(Collectors.toList());
                                    op.getImplementation().setDependencies(dependencies);
                                }
                            });
                        }
                    });
                }
            });
        // convert locations of artifacts
        entryDefinitions.getRelationshipTypes()
            .stream()
            .filter(nt -> nt.getQName().equals(node.getQName()))
            .forEach(nt -> {
                if (nt.getInterfaceDefinitions() != null) {
                    nt.getInterfaceDefinitions().forEach(interfaceDefinition -> {
                        if (interfaceDefinition.getOperations() != null) {
                            interfaceDefinition.getOperations().forEach(op -> {
                                if (op.getImplementation() != null && op.getImplementation().getPrimary() != null) {
                                    String artifactName = op.getImplementation().getPrimary();
                                    RepositoryFileReference ref = new RepositoryFileReference(id, p, artifactName);
                                    String pathInsideRepo = BackendUtils.getPathInsideRepo(ref);
                                    putRefAsReferencedItemInCsar(repository, ref);
                                    op.getImplementation().setPrimary("/" + FilenameUtils.separatorsToUnix(pathInsideRepo));
                                }
                            });
                        }
                    });
                }
            });
    }

    private void prepareNodeTypeForExport(IRepository repository, NodeTypeId id, TDefinitions entryDefinitions) {
        //refMap.put(new CsarContentProperties(BackendUtils.getPathInsideRepo(licenseRef)), new RepositoryRefBasedCsarEntry(licenseRef));
        String nodeTypePath = BackendUtils.getPathInsideRepo(id);
        TNodeType node = repository.getElement(id);
        List<TArtifact> artifacts = node.getArtifacts();
        if (Objects.nonNull(artifacts)) {
            artifacts.forEach(artifact -> {
                UrlValidator customValidator = new UrlValidator();
                if (customValidator.isValid(artifact.getFile())) {
                    LOGGER.info("Specified file is a valid URL, start processing the reference");
                    try {
                        String pathInsideRepo = putRemoteRefAsReferencedItemInCsar(new URL(artifact.getFile()), nodeTypePath, artifact.getName());
                        updatePathsInNodeArtifacts(entryDefinitions, node, artifact, pathInsideRepo);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                } else {
                    Path p = Paths.get("files", artifact.getId());
                    RepositoryFileReference ref = new RepositoryFileReference(id, p, artifact.getFile());
                    if (repository.exists(ref)) {
                        putRefAsReferencedItemInCsar(repository, ref);
                        String pathInsideRepo = BackendUtils.getPathInsideRepo(ref);
                        updatePathsInNodeArtifacts(entryDefinitions, node, artifact, pathInsideRepo);
                    }
                }
            });
        }
    }

    private void updatePathsInNodeArtifacts(TDefinitions entryDefinitions, TNodeType node, TArtifact artifact, String pathInsideRepo) {
        // update file paths in "artifacts" the exported service template
        entryDefinitions.getNodeTypes()
            .stream()
            .filter(nt -> nt.getQName().equals(node.getQName()))
            .forEach(nt -> {
                if (nt.getArtifacts() != null) {
                    nt.getArtifacts().stream()
                        .filter(art -> art.getFile().equals(artifact.getFile()))
                        .forEach(art -> {
                            art.setFile("/" + FilenameUtils.separatorsToUnix(pathInsideRepo));
                        });
                }

                if (nt.getInterfaceDefinitions() != null) {
                    nt.getInterfaceDefinitions().forEach(interfaceDefinition -> {
                        if (interfaceDefinition.getOperations() != null) {
                            interfaceDefinition.getOperations().forEach(op -> {
                                if (op.getImplementation() != null) {
                                    // update "primary" field in the exported service template
                                    String artifactName = op.getImplementation().getPrimary();
                                    if (artifactName.equalsIgnoreCase(artifact.getName())) {
                                        op.getImplementation().setPrimary("/" + FilenameUtils.separatorsToUnix(pathInsideRepo));
                                    }

                                    // update "dependencies" field in the exported service template
                                    if (op.getImplementation().getDependencies() != null) {
                                        List<String> dependencies = op.getImplementation().getDependencies().stream().map(aName -> {
                                            if (aName.equalsIgnoreCase(artifact.getName())) {
                                                return "/" + FilenameUtils.separatorsToUnix(pathInsideRepo);
                                            }
                                            return aName;
                                        }).collect(Collectors.toList());
                                        op.getImplementation().setDependencies(dependencies);
                                    }
                                }
                            });
                        }
                    });
                }
            });
    }

    /**
     * Prepares artifacts in Service Template
     */
    private void prepareServiceTemplateForExport(IRepository repository, ServiceTemplateId id, TDefinitions entryDefinitions) throws IOException {
        BackendUtils.synchronizeReferences(id, repository);
        TServiceTemplate st = repository.getElement(id);
        String serviceTemplatePath = BackendUtils.getPathInsideRepo(id);

        if (Objects.nonNull(st.getTopologyTemplate())) {
            for (TNodeTemplate nodeTemplate : st.getTopologyTemplate().getNodeTemplates()) {
                List<TArtifact> artifacts = nodeTemplate.getArtifacts();
                if (Objects.nonNull(artifacts)) {

                    // update file paths in the exported service template
                    artifacts.forEach(a -> {
                        UrlValidator customValidator = new UrlValidator();
                        if (customValidator.isValid(a.getFile())) {
                            LOGGER.info("Specified file is a valid URL, start processing the reference");
                            try {
                                String templateArtifactPath = nodeTemplate.getId() + "/" + a.getName();
                                String pathInsideRepo = putRemoteRefAsReferencedItemInCsar(new URL(a.getFile()), serviceTemplatePath, templateArtifactPath);
                                updatePathsInTopologyTemplateArtifacts(entryDefinitions, nodeTemplate, a, pathInsideRepo);
                            } catch (MalformedURLException e) {
                                LOGGER.warn("Supplied URL is invalid: {}", e.getMessage());
                            }
                        } else {
                            Path p = Paths.get("files", nodeTemplate.getId(), a.getId());
                            RepositoryFileReference ref = new RepositoryFileReference(id, p, a.getFile());
                            try {
                                if (repository.exists(ref)) {
                                    putRefAsReferencedItemInCsar(repository, ref);
                                    String pathInsideRepo = BackendUtils.getPathInsideRepo(ref);
                                    updatePathsInTopologyTemplateArtifacts(entryDefinitions, nodeTemplate, a, pathInsideRepo);
                                }
                            } catch (Exception e) {
                                LOGGER.warn("Could not add artifact reference: {}", e.getMessage());
                            }
                        }
                    });

                    // TODO: update "primary" field in the exported service template
                }
            }
        }
    }

    private void updatePathsInTopologyTemplateArtifacts(TDefinitions entryDefinitions, TNodeTemplate nodeTemplate, TArtifact artifact, String pathInsideRepo) {
        entryDefinitions.getServiceTemplates()
            .stream().filter(Objects::nonNull)
            .findFirst()
            .ifPresent(s -> {
                if (Objects.nonNull(s.getTopologyTemplate())) {
                    s.getTopologyTemplate()
                        .getNodeTemplates()
                        .stream()
                        .filter(node -> node.getId().equals(nodeTemplate.getId()))
                        .forEach(node -> node.getArtifacts()
                            .stream()
                            .filter(art -> art.getFile().equals(artifact.getFile()))
                            .forEach(art -> {
                                art.setFile("/" + FilenameUtils.separatorsToUnix(pathInsideRepo));
                            }));
                }
            });
    }

    /**
     * Puts the given reference as item in the CSAR
     * <p>
     * Thereby, it uses the global variable referencesToPathInCSARMap
     */
    protected String putRemoteRefAsReferencedItemInCsar(URL url, String typePath, String artifactName) {
        RemoteRefBasedCsarEntry ref = new RemoteRefBasedCsarEntry(url);
        String fileName = FilenameUtils.getName(url.getPath());
        String type = StringUtils.removeEnd(typePath, "/");
        String path = StringUtils.join(Arrays.asList(type, "files", artifactName, fileName), "/");
        this.referencesToPathInCSARMap.put(new CsarContentProperties(path), ref);
        return path;
    }
}
