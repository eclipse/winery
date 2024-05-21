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

package org.eclipse.winery.repository.importing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.winery.model.converter.support.Namespaces;
import org.eclipse.winery.model.converter.support.exception.MultiException;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFile;
import org.eclipse.winery.model.csar.toscametafile.YamlTOSCAMetaFileParser;
import org.eclipse.winery.model.ids.IdNames;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TArtifact;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.model.tosca.yaml.YTServiceTemplate;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.constants.Filename;
import org.eclipse.winery.repository.backend.filebased.GitBasedRepository;
import org.eclipse.winery.repository.common.RepositoryFileReference;
import org.eclipse.winery.repository.common.Util;
import org.eclipse.winery.repository.converter.reader.YamlReader;
import org.eclipse.winery.repository.converter.writer.YamlWriter;
import org.eclipse.winery.repository.datatypes.ids.elements.DirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.GenericDirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.VisualAppearanceId;
import org.eclipse.winery.repository.yaml.YamlRepository;
import org.eclipse.winery.repository.yaml.converter.FromCanonical;
import org.eclipse.winery.repository.yaml.converter.ToCanonical;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlCsarImporter extends CsarImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlCsarImporter.class);

    private final YamlRepository targetRepository;

    private final Set<TImport> handledImports = new HashSet<>();

    public YamlCsarImporter(YamlRepository target) {
        super(target);
        this.targetRepository = target;
    }

    /**
     * Parse TOSCA Meta File
     *
     * @param toscaMetaPath the path of the meta file
     */
    @Override
    protected TOSCAMetaFile parseTOSCAMetaFile(Path toscaMetaPath) {
        final YamlTOSCAMetaFileParser tmfp = new YamlTOSCAMetaFileParser();
        return tmfp.parse(toscaMetaPath);
    }

    @Override
    protected Optional<TDefinitions> parseDefinitionsElement(Path entryDefinitionsPath, final List<String> errors) {
        YamlReader reader = new YamlReader();
        YTServiceTemplate serviceTemplate;
        try {
            serviceTemplate = reader.parse(new FileInputStream(entryDefinitionsPath.toFile()));

            String name = serviceTemplate.getMetadata().get("name");
            if (name == null) {
                // fallback to filename
                name = entryDefinitionsPath.toString().substring(entryDefinitionsPath.toString().indexOf("__") + 2, entryDefinitionsPath.toString().indexOf(".tosca"));
            }

            ToCanonical converter = new ToCanonical(targetRepository);
            return Optional.of(converter.convert(serviceTemplate, name, serviceTemplate.getMetadata().get("targetNamespace"), true));
        } catch (MultiException | FileNotFoundException e) {
            e.printStackTrace();
            LOGGER.error("Could not read the given entry definition " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    protected void parseCsarContents(final Path path, TOSCAMetaFile tmf, ImportMetaInformation importMetaInformation, CsarImportOptions options, Map<String, File> fileMap) throws IOException {
        String entryDefinitions = tmf.getEntryDefinitions();

        if (Objects.nonNull(entryDefinitions)) {

            Path defsPath = path.resolve(entryDefinitions);
            importMetaInformation.entryServiceTemplate = null;
            this.importDefinitions(tmf, defsPath, importMetaInformation.errors, options)
                .ifPresent(serviceTemplateId1 -> importMetaInformation.entryServiceTemplate = serviceTemplateId1);

            this.importSelfServiceMetaData(tmf, path, defsPath, importMetaInformation.errors);
        }
    }

    @Override
    protected Optional<ServiceTemplateId> processDefinitionsImport(TDefinitions defs, TOSCAMetaFile tmf, Path definitionsPath, List<String> errors, CsarImportOptions options) throws IOException {
        Optional<ServiceTemplateId> entryServiceTemplate = Optional.empty();

        String defaultNamespace = defs.getTargetNamespace();
        List<TExtensibleElements> componentInstanceList = defs.getServiceTemplateOrNodeTypeOrNodeTypeImplementation();

        for (final TExtensibleElements ci : componentInstanceList) {
            if (ci instanceof org.eclipse.winery.model.tosca.TServiceTemplate
                && Objects.isNull(((org.eclipse.winery.model.tosca.TServiceTemplate) ci).getTopologyTemplate())) {
                continue;
            }

            // Determine & ensure that element has the namespace
            String namespace = this.getNamespace(ci, defaultNamespace);
            this.setNamespace(ci, namespace);
            String id = ModelUtilities.getId(ci);

            final DefinitionsChildId wid = determineWineryId(ci, namespace, id);

            if (targetRepository.exists(wid)) {
                if (options.isOverwrite()) {
                    targetRepository.forceDelete(wid);
                    String msg = String.format("Deleted %1$s %2$s to enable replacement", ci.getClass().getName(), wid.getQName().toString());
                    LOGGER.debug(msg);
                } else {
                    String msg = String.format("Skipped %1$s %2$s, because it already exists", ci.getClass().getName(), wid.getQName().toString());
                    LOGGER.debug(msg);
                    // this is not displayed in the UI as we currently do not distinguish between pre-existing types and types created during the import.
                    continue;
                }
            }

            // Create a fresh definitions object without the other data.
            final TDefinitions newDefs = BackendUtils.createWrapperDefinitions(wid, targetRepository);
            // add the current TExtensibleElements as the only content to it
            newDefs.getServiceTemplateOrNodeTypeOrNodeTypeImplementation().add(ci);

            // import license and readme files
            importLicenseAndReadme(definitionsPath.getParent().getParent(), wid, tmf, errors);
            importArtifacts(definitionsPath.getParent().getParent(), ci, wid, tmf, errors);

            if (ci instanceof TNodeType) {
                this.adjustNodeType(definitionsPath.getParent().getParent(), (TNodeType) ci, (NodeTypeId) wid, tmf, errors);
            } else if (ci instanceof TRelationshipType) {
                this.adjustRelationshipType(definitionsPath.getParent().getParent(), (TRelationshipType) ci, (RelationshipTypeId) wid, tmf, errors);
            } else if (ci instanceof TServiceTemplate) {
                // tosca yaml doesn't have plans but workflows, therefore this seems not to be working properly
               // this.adjustServiceTemplate(definitionsPath.getParent().getParent(), tmf, (ServiceTemplateId) wid, (TServiceTemplate) ci, errors);
                entryServiceTemplate = Optional.of((ServiceTemplateId) wid);
            }

            storeDefs(wid, newDefs);
        }

        List<TImport> imports = defs.getImport();
        this.importImports(definitionsPath.getParent(), tmf, imports, errors, options);

        return entryServiceTemplate;
    }

    private void importArtifacts(Path rootPath, TExtensibleElements ci, DefinitionsChildId wid, TOSCAMetaFile tmf, final List<String> errors) {
        if (ci instanceof TServiceTemplate) {
            TServiceTemplate st = (TServiceTemplate) ci;
            if (st.getTopologyTemplate() != null) {
                st.getTopologyTemplate()
                    .getNodeTemplates()
                    .forEach(node -> {
                        if (Objects.nonNull(node.getArtifacts()) && !node.getArtifacts().isEmpty()) {
                            node.getArtifacts()
                                .stream()
                                .map(this::fixForwardSlash)
                                .filter(a -> this.isImportable(rootPath, a))
                                .forEach(a -> {
                                    DirectoryId stFilesDir = new GenericDirectoryId(wid, IdNames.FILES_DIRECTORY);
                                    DirectoryId ntFilesDir = new GenericDirectoryId(stFilesDir, node.getId());
                                    DirectoryId artifactDir = new GenericDirectoryId(ntFilesDir, a.getName());
                                    importArtifact(rootPath, a, artifactDir, tmf, errors);
                                    fixArtifactRefName(rootPath, a);
                                });
                        }
                    });
            }
        } else if (ci instanceof TNodeType) {
            TNodeType nt = (TNodeType) ci;

            fixOperationImplFileRef(nt);

            if (Objects.nonNull(nt.getArtifacts()) && !nt.getArtifacts().isEmpty()) {
                nt.getArtifacts().stream()
                    .map(this::fixForwardSlash)
                    .filter(a -> this.isImportable(rootPath, a))
                    .forEach(a -> {
                        DirectoryId typeFilesDir = new GenericDirectoryId(wid, IdNames.FILES_DIRECTORY);
                        DirectoryId artifactDir = new GenericDirectoryId(typeFilesDir, a.getName());
                        importArtifact(rootPath, a, artifactDir, tmf, errors);
                        fixArtifactRefName(rootPath, a);
                    });
            }
        }
    }

    private TArtifact fixForwardSlash(TArtifact a) {
        a.setFile(fixForwardSlash(a.getFile()));
        return a;
    }

    private String fixForwardSlash(String filePath) {
        if (filePath.startsWith("/")) {
            return filePath.replaceFirst("/", "");
        }

        return filePath;
    }

    private void fixArtifactRefName(Path rootPath, TArtifact a) {
        // store only the file name in the definitions (current Winery's file referencing style)
        String fileRef = rootPath.resolve(a.getFile()).getFileName().toString();
        a.setFile(fileRef);
    }

    private void fixOperationImplFileRef(TNodeType nt) {
        // current assumption: primary field in operation's implementation stores file reference
        if (Objects.nonNull(nt.getArtifacts()) && Objects.nonNull(nt.getInterfaceDefinitions())) {
            nt.getArtifacts().forEach(a -> {
                nt.getInterfaceDefinitions().forEach(iDef -> {
                    if (Objects.nonNull(iDef.getOperations())) {
                        iDef.getOperations().forEach(op -> {
                            if (op.getImplementation().getPrimary().equals(a.getFile())) {
                                op.getImplementation().setPrimary(a.getName());
                            }

                            if (Objects.nonNull(op.getImplementation().getDependencies())) {
                                List<String> updatedDependencies = op.getImplementation()
                                    .getDependencies()
                                    .stream()
                                    .map(dep -> {
                                        if (a.getFile().equals(dep)) {
                                            return a.getName();
                                        }
                                        return dep;
                                    }).collect(Collectors.toList());
                                op.getImplementation().setDependencies(updatedDependencies);
                            }
                        });
                    }
                });
            });
        }
    }

    private boolean isImportable(Path rootPath, TArtifact a) {
        String filename = a.getFile();
        if (filename.matches(".*[/\n\r\t\0\f`?*\\\\<>|\":].*")) {
            LOGGER.debug("Invalid filename ({}), skipping it", filename);
            return false;
        }
        Path path = rootPath.resolve(filename);
        if (!Files.exists(path)) {
            LOGGER.warn("Reference {} not found, skipping it", filename);
            return false;
        }
        return Files.isRegularFile(path);
    }

    private void importArtifact(Path rootPath, TArtifact a, DirectoryId artifactDir, TOSCAMetaFile tmf, final List<String> errors) {
        String fileName = rootPath.resolve(a.getFile()).getFileName().toString();
        RepositoryFileReference ref = new RepositoryFileReference(artifactDir, fileName);
        importFile(rootPath.resolve(a.getFile()), ref, tmf, rootPath, errors);
    }

    /**
     * @param basePath the base path where to resolve files from. This is the directory of the Definitions
     * @param imports  the list of imports to import. SIDE EFFECT: this list is modified. After this method has run, the
     *                 list contains the imports to be put into the wrapper element
     * @param options  the set of options applicable while importing a CSAR
     */
    private void importImports(Path basePath, TOSCAMetaFile tmf, List<TImport> imports, final List<String> errors, CsarImportOptions options) throws IOException {
        for (TImport imp : imports) {
            if (handledImports.contains(imp)) {
                continue;
            }
            handledImports.add(imp);
            String importType = imp.getImportType();
            String location = imp.getLocation();
            if (Namespaces.TOSCA_YAML_NS.equals(importType)) {
                Path defsPath = basePath.resolve(location);
                // fallback for older CSARs, where the location is given from the root
                if (Files.exists(defsPath)) {
                    this.importDefinitions(tmf, defsPath, errors, options);
                    // imports of definitions don't have to be kept as these are managed by Winery
                }
            }
        }
    }

    private void storeDefs(DefinitionsChildId id, TDefinitions defs) {
        RepositoryFileReference ref = BackendUtils.getRefOfDefinitions(id);
        IRepository repo = RepositoryFactory.getRepository();
        FromCanonical converter = null;
        if (repo instanceof GitBasedRepository) {
            GitBasedRepository wrapper = (GitBasedRepository) RepositoryFactory.getRepository();
            converter = new FromCanonical((YamlRepository) wrapper.getRepository());
        } else if (repo instanceof YamlRepository) {
            converter = new FromCanonical((YamlRepository) repo);
        }

        if (Objects.nonNull(converter)) {
            YamlWriter writer = new YamlWriter();
            writer.write(converter.convert((TDefinitions) defs), repo.ref2AbsolutePath(ref));
        }
    }

    @Override
    protected void importIcons(Path rootPath, VisualAppearanceId visId, TOSCAMetaFile tmf, final List<String> errors) {
        String pathInsideRepo = Util.getPathInsideRepo(visId);
        Path visPath = rootPath.resolve(pathInsideRepo);
        this.importIcon(visId, visPath, Filename.FILENAME_BIG_ICON, tmf, rootPath, errors);
        this.importIcon(visId, visPath, Filename.FILENAME_SMALL_ICON, tmf, rootPath, errors);
    }
}
