/*******************************************************************************
 * Copyright (c) 2012-2020 Contributors to the Eclipse Foundation
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.winery.accountability.AccountabilityManager;
import org.eclipse.winery.accountability.AccountabilityManagerFactory;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.accountability.model.ProvenanceVerification;
import org.eclipse.winery.common.Constants;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFile;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileParser;
import org.eclipse.winery.model.ids.EncodingUtil;
import org.eclipse.winery.model.ids.XmlId;
import org.eclipse.winery.model.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.ids.definitions.EntityTypeId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.ids.definitions.imports.GenericImportId;
import org.eclipse.winery.model.ids.definitions.imports.XSDImportId;
import org.eclipse.winery.model.ids.elements.PlanId;
import org.eclipse.winery.model.ids.elements.PlansId;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactReference.Exclude;
import org.eclipse.winery.model.tosca.TArtifactReference.Include;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.model.tosca.TDefinitions.Types;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TPlan;
import org.eclipse.winery.model.tosca.TPlan.PlanModelReference;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.model.version.VersionSupport;
import org.eclipse.winery.repository.JAXBSupport;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.NamespaceManager;
import org.eclipse.winery.repository.backend.constants.Filename;
import org.eclipse.winery.repository.backend.constants.MediaTypes;
import org.eclipse.winery.repository.backend.filebased.ConfigurationBasedNamespaceManager;
import org.eclipse.winery.repository.backend.filebased.FileUtils;
import org.eclipse.winery.repository.backend.filebased.JsonBasedNamespaceManager;
import org.eclipse.winery.repository.backend.filebased.NamespaceProperties;
import org.eclipse.winery.repository.backend.xsd.XsdImportManager;
import org.eclipse.winery.repository.common.RepositoryFileReference;
import org.eclipse.winery.repository.common.Util;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateFilesDirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.DirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.SelfServiceMetaDataId;
import org.eclipse.winery.repository.datatypes.ids.elements.VisualAppearanceId;
import org.eclipse.winery.repository.export.CsarExporter;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

/**
 * Imports a CSAR into the storage. As the internal storage format does not have CSARs as the topmost artifacts, but one
 * TDefinition, the CSAR has to be split up into several components.
 * <p>
 * Existing components are <em>not</em> replaced, but silently skipped
 * <p>
 * Minor errors are logged and not further propagated / notified. That means, a user cannot see minor errors. Major
 * errors are immediately thrown.
 * <p>
 * One instance for each import
 */
// FIXME the Importer needs to become standard-aware. CSARs can be built to differing standards, which are mutually incompatible.
//  As such the importing process needs to be aware of the standard to which an imported CSAR is built and, if necessary, convert the imported CSAR to the underlying storage.
// FIXME consider using an XMLRepository and a YAMLRepository encapsulated in a MultiRepository to store Definitions of arbitrary standards.
//  Conversions between the standards should be held to a minimum, since there are mutual incompatibilities
public class CsarImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsarImporter.class);

    // ExecutorService for XSD schema initialization
    // Threads set to 1 to avoid testing for parallel processing of the same XSD file
    private static final ExecutorService xsdParsingService = Executors.newFixedThreadPool(1);

    private static final ExecutorService entityTypeAdjustmentService = Executors.newFixedThreadPool(10);

    private static final Pattern GENERATED_PREFIX_PATTERN = Pattern.compile("^ns\\d+$");

    private final IRepository targetRepository;

    public CsarImporter(IRepository targetRepository) {
        this.targetRepository = targetRepository;
    }

    /**
     * All EntityTypes may contain properties definition. In case a winery properties definition is found, the TOSCA
     * conforming properties definition is removed
     *
     * @param ci      the entity type
     * @param wid     the Winery id of the entityType
     * @param newDefs the definitions, the entity type is contained in. The imports might be adjusted here
     * @param errors  Used to collect the errors
     */
    private void adjustEntityType(TEntityType ci, EntityTypeId wid, TDefinitions newDefs, final List<String> errors) {
        TEntityType.PropertiesDefinition propertiesDefinition = ci.getProperties();
        if (propertiesDefinition != null) {
            WinerysPropertiesDefinition winerysPropertiesDefinition = ci.getWinerysPropertiesDefinition();
            boolean deriveWPD;
            if (winerysPropertiesDefinition == null) {
                deriveWPD = true;
            } else {
                if (winerysPropertiesDefinition.getIsDerivedFromXSD() == null) {
                    // no derivation from properties required as the properties are generated by Winery
                    deriveWPD = false;

                    // we have to remove the import, too
                    // Determine the location
                    String elementName = winerysPropertiesDefinition.getElementName();
                    String loc = BackendUtils.getImportLocationForWinerysPropertiesDefinitionXSD(wid, null, elementName);
                    // remove the import matching that location
                    List<TImport> imports = newDefs.getImport();
                    boolean found = false;
                    if (imports != null) {
                        Iterator<TImport> iterator = imports.iterator();
                        TImport imp;
                        while (iterator.hasNext()) {
                            imp = iterator.next();
                            // TODO: add check for QNames.QNAME_WINERYS_PROPERTIES_DEFINITION_ATTRIBUTE instead of import location. The current routine, however, works, too.
                            if (imp.getLocation().equals(loc)) {
                                found = true;
                                break;
                            }
                        }
                        //noinspection StatementWithEmptyBody
                        if (found) {
                            // imp with Winery's k/v location found
                            iterator.remove();
                            // the XSD has been imported in importOtherImport
                            // it was too difficult to do the location check there, therefore we just remove the XSD from the repository here
                            XSDImportId importId = new XSDImportId(winerysPropertiesDefinition.getNamespace(), elementName, false);
                            try {
                                this.targetRepository.forceDelete(importId);
                            } catch (IOException e) {
                                CsarImporter.LOGGER.debug("Could not delete Winery's generated XSD definition", e);
                                errors.add("Could not delete Winery's generated XSD definition");
                            }
                        } else {
                            // K/V properties definition was incomplete
                        }
                    }
                } else {
                    // winery's properties are derived from an XSD
                    // The export does NOT add an imports statement: only the wpd exists
                    // We remove that as
                    ModelUtilities.removeWinerysPropertiesDefinition(ci);
                    // derive the WPDs again from the properties definition
                    deriveWPD = true;
                }
            }
            if (deriveWPD) {
                BackendUtils.deriveWPD(ci, errors, targetRepository);
            }
        }
    }

    /**
     * Imports a file from the filesystem to the repository
     *
     * @param p                       the file to read from
     * @param repositoryFileReference the "file" to put the content to
     * @param tmf                     the TOSCAMetaFile object used to determine the mimetype. Must not be null.
     * @param rootPath                used to make the path p relative in order to determine the mime type
     * @param errors                  list where import errors should be stored to
     */
    protected void importFile(Path p, RepositoryFileReference repositoryFileReference, TOSCAMetaFile tmf, Path rootPath, final List<String> errors) {
        Objects.requireNonNull(p);
        Objects.requireNonNull(repositoryFileReference);
        Objects.requireNonNull(tmf);
        Objects.requireNonNull(rootPath);
        Objects.requireNonNull(errors);
        try (InputStream is = Files.newInputStream(p); BufferedInputStream bis = new BufferedInputStream(is)) {
            MediaType mediaType = MediaType.parse(tmf.getMimeType(p.relativize(rootPath).toString()));
            if (mediaType == null) {
                // Manually find out mime type
                try {
                    mediaType = BackendUtils.getMimeType(bis, p.getFileName().toString());
                } catch (IOException e) {
                    errors.add(String.format("No MimeType given for %1$s (%2$s)", p.getFileName(), e.getMessage()));
                    return;
                }
                if (mediaType == null) {
                    errors.add(String.format("No MimeType given for %1$s", p.getFileName()));
                    return;
                }
            }
            try {
                targetRepository.putContentToFile(repositoryFileReference, bis, mediaType);
            } catch (IllegalArgumentException | IOException e) {
                throw new IllegalStateException(e);
            }
        } catch (IOException e1) {
            throw new IllegalStateException("Could not work on generated temporary files", e1);
        }
    }

    public static void storeDefinitions(IRepository repository, DefinitionsChildId id, TDefinitions defs) {
        RepositoryFileReference ref = BackendUtils.getRefOfDefinitions(id);
        String s = BackendUtils.getXMLAsString(defs, true, repository);
        try {
            repository.putContentToFile(ref, s, MediaTypes.MEDIATYPE_TOSCA_DEFINITIONS);
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Reads the CSAR from the given inputStream
     *
     * @param in      the inputStream to read from
     * @param options the set of options applicable for importing the csar
     */
    public ImportMetaInformation readCSAR(InputStream in, CsarImportOptions options)
        throws IOException, AccountabilityException, ExecutionException, InterruptedException {
        // we have to extract the file to a temporary directory as
        // the .definitions file does not necessarily have to be the first entry in the archive
        Path csarDir = Files.createTempDirectory("winery");
        Map<String, File> fileMap = null;

        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    Path targetPath = csarDir.resolve(entry.getName());
                    if (!targetPath.normalize().startsWith(csarDir.normalize())) {
                        throw new IOException("Bad zip entry");
                    }
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath);
                    if (options.isValidate()) {
                        if (Objects.isNull(fileMap)) {
                            fileMap = new HashMap<>();
                        }
                        fileMap.put(entry.getName(), targetPath.toFile());
                    }
                }
            }

            return this.importFromDir(csarDir, options, fileMap);
        } catch (AccountabilityException e) {
            LOGGER.debug("Error while checking the accountability of the CSAR");
            throw e;
        } catch (IOException e) {
            CsarImporter.LOGGER.debug("Could not import CSAR", e);
            throw e;
        } finally {
            // cleanup: delete all contents of the temporary directory
            FileUtils.forceDelete(csarDir);
        }
    }

    /**
     * Import an extracted CSAR from a directory
     *
     * @param path    the root path of an extracted CSAR file
     * @param options the set of options applicable while importing a CSAR
     * @param fileMap Contains all files which were extracted from the CSAR and have to be validated using the
     *                accountability layer
     */
    private ImportMetaInformation importFromDir(final Path path, CsarImportOptions options,
                                                Map<String, File> fileMap) throws IOException, AccountabilityException, ExecutionException, InterruptedException {
        final ImportMetaInformation importMetaInformation = new ImportMetaInformation();
        Path toscaMetaPath = path.resolve("TOSCA-Metadata/TOSCA.meta");
        if (!Files.exists(toscaMetaPath)) {
            importMetaInformation.errors.add("TOSCA.meta does not exist");
            return importMetaInformation;
        }

        final TOSCAMetaFile tmf = parseTOSCAMetaFile(toscaMetaPath);
        parseCsarContents(path, tmf, importMetaInformation, options, fileMap);

        if (importMetaInformation.errors.isEmpty()) {
            importNamespacePrefixes(path);
        }

        return importMetaInformation;
    }

    protected void parseCsarContents(final Path path, TOSCAMetaFile tmf, ImportMetaInformation importMetaInformation, CsarImportOptions options, Map<String, File> fileMap) throws IOException, InterruptedException, ExecutionException, AccountabilityException {
        // we do NOT do any sanity checks, of TOSCA.meta
        // and just start parsing        
        if (tmf.getEntryDefinitions() != null) {

            // we obey the entry definitions and "just" import that
            // imported definitions are added recursively
            Path defsPath = path.resolve(tmf.getEntryDefinitions());
            importMetaInformation.entryServiceTemplate = null;
            this.importDefinitions(tmf, defsPath, importMetaInformation.errors, options)
                .ifPresent(serviceTemplateId1 -> importMetaInformation.entryServiceTemplate = serviceTemplateId1);

            // we assume that the entry definition identifies the provenance element
            if (Objects.nonNull(fileMap)) {
                if (!(importMetaInformation.valid = this.isValid(importMetaInformation, fileMap))) {
                    return;
                }
            }

            this.importSelfServiceMetaData(tmf, path, defsPath, importMetaInformation.errors);
        } else {
            // no explicit entry definitions found
            // we import all available definitions
            // The specification says (cos01, Section 16.1, line 2935) that all definitions are contained in the "Definitions" directory
            // The alternative is to go through all entries in the TOSCA Meta File, but there is no guarantee that this list is complete
            Path definitionsDir = path.resolve("Definitions");
            if (!Files.exists(definitionsDir)) {
                importMetaInformation.errors.add("No entry definitions defined and Definitions directory does not exist.");
                return;
            }
            final List<IOException> exceptions = new ArrayList<>();
            Files.walkFileTree(definitionsDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.endsWith("Definitions")) {
                        return FileVisitResult.CONTINUE;
                    } else {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        CsarImporter.this.importDefinitions(tmf, file, importMetaInformation.errors, options);
                    } catch (IOException e) {
                        exceptions.add(e);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            if (!exceptions.isEmpty()) {
                // something went wrong during parsing
                // we rethrow the exception
                throw exceptions.get(0);
            }
        }
    }

    /**
     * Parse TOSCA Meta File
     *
     * @param toscaMetaPath path of the meta file
     */
    protected TOSCAMetaFile parseTOSCAMetaFile(Path toscaMetaPath) {
        final TOSCAMetaFileParser tmfp = new TOSCAMetaFileParser();
        return tmfp.parse(toscaMetaPath);
    }

    private boolean isValid(ImportMetaInformation metaInformation, Map<String, File> fileMap)
        throws ExecutionException, InterruptedException, AccountabilityException {
        ServiceTemplateId entryServiceTemplate = metaInformation.entryServiceTemplate;
        metaInformation.verificationMap = new HashMap<>();

        if (Objects.nonNull(entryServiceTemplate)) {
            AccountabilityManager accountabilityManager = AccountabilityManagerFactory.getAccountabilityManager();
            String provenanceIdentifier = VersionSupport.getQNameWithComponentVersionOnly(entryServiceTemplate);

            metaInformation.verificationMap = accountabilityManager
                .verify(provenanceIdentifier, "TOSCA-Metadata/TOSCA.meta", fileMap)
                .exceptionally(e -> {
                    LOGGER.debug("accountabilityManager.verify completed exceptionally", e);
                    return null;
                })
                .get();

            return metaInformation.verificationMap.values().stream().allMatch(v -> v == ProvenanceVerification.VERIFIED);
        }

        return false;
    }

    /**
     * Import namespace prefixes. This is kind of a quick hack. TODO: during the import, the prefixes should be
     * extracted using JAXB and stored in the NamespacesResource
     *
     * @param rootPath the root path of the extracted CSAR
     */
    private void importNamespacePrefixes(Path rootPath) {
        NamespaceManager namespaceManager = targetRepository.getNamespaceManager();
        Path properties = rootPath.resolve(CsarExporter.PATH_TO_NAMESPACES_PROPERTIES);
        Path json = rootPath.resolve(CsarExporter.PATH_TO_NAMESPACES_JSON);

        if (Files.exists(properties) || Files.exists(json)) {
            NamespaceManager localNamespaceManager;

            if (Files.exists(properties)) {
                PropertiesConfiguration pconf = new PropertiesConfiguration();
                try (final BufferedReader propertyReader = Files.newBufferedReader(properties)) {
                    pconf.read(propertyReader);
                    localNamespaceManager = new ConfigurationBasedNamespaceManager(pconf);
                } catch (IOException | ConfigurationException e) {
                    CsarImporter.LOGGER.debug(e.getMessage(), e);
                    return;
                }
            } else {
                localNamespaceManager = new JsonBasedNamespaceManager(json.toFile());
            }

            for (String s : localNamespaceManager.getAllNamespaces().keySet()) {
                boolean addToStorage = false;
                String namespace = s;
                if (namespaceManager.hasPermanentProperties(namespace)) {
                    String storedPrefix = namespaceManager.getPrefix(namespace);
                    // QUICK HACK to check whether the prefix is a generated one
                    // We assume we know the internal generation routine
                    Matcher m = CsarImporter.GENERATED_PREFIX_PATTERN.matcher(storedPrefix);
                    if (m.matches()) {
                        // the stored prefix is a generated one
                        // replace it by the one stored in the exported properties
                        addToStorage = true;
                    }
                } else {
                    addToStorage = true;
                }
                if (addToStorage) {
                    String prefix = localNamespaceManager.getPrefix(namespace);
                    namespaceManager.setNamespaceProperties(namespace, new NamespaceProperties(namespace, prefix));
                }
            }
        }
    }

    /**
     * Imports a self-service meta data description (if available)
     * <p>
     * The first service template in the provided entry definitions is taken
     */
    protected void importSelfServiceMetaData(final TOSCAMetaFile tmf, final Path rootPath, Path entryDefinitions, final List<String> errors) {
        final Path selfServiceDir = rootPath.resolve(Constants.DIRNAME_SELF_SERVICE_METADATA);
        if (!Files.exists(selfServiceDir)) {
            CsarImporter.LOGGER.debug("Self-service Portal directory does not exist in CSAR");
            return;
        }
        if (!Files.exists(entryDefinitions)) {
            CsarImporter.LOGGER.debug("Entry definitions does not exist.");
            return;
        }

        Optional<TDefinitions> result = parseDefinitionsElement(entryDefinitions, errors);

        if (result.isPresent()) {
            TDefinitions defs = result.get();

            final int cutLength = selfServiceDir.toString().length() + 1;
            Iterator<TExtensibleElements> iterator = defs.getServiceTemplateOrNodeTypeOrNodeTypeImplementation().iterator();
            boolean found = false;
            TExtensibleElements next = null;
            while (iterator.hasNext() && !found) {
                next = iterator.next();
                if (next instanceof TServiceTemplate) {
                    found = true;
                }
            }

            if (found) {
                TServiceTemplate serviceTemplate = (TServiceTemplate) next;
                String namespace = serviceTemplate.getTargetNamespace();
                if (namespace == null) {
                    namespace = defs.getTargetNamespace();
                }
                ServiceTemplateId stId = new ServiceTemplateId(namespace, serviceTemplate.getId(), false);
                final SelfServiceMetaDataId id = new SelfServiceMetaDataId(stId);

                // QUICK HACK: We just import all data without any validation
                // Reason: the metadata resource can deal with nearly arbitrary formats of the data, therefore we do not do any checking here

                try {
                    Files.walkFileTree(selfServiceDir, new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            String name = file.toString().substring(cutLength);
                            // check: if name contains "/", this could lead to exceptions
                            RepositoryFileReference ref = new RepositoryFileReference(id, name);

                            if (name.equals("data.xml")) {
                                // we have to check whether the data.xml contains
                                // (uri:"http://opentosca.org/self-service", local:"application")
                                // instead of
                                // (uri:"http://www.eclipse.org/winery/model/selfservice", local:"Application"
                                // We quickly replace it via String replacement instead of XSLT
                                try {
                                    String oldContent = org.apache.commons.io.FileUtils.readFileToString(file.toFile(), "UTF-8");
                                    String newContent = oldContent.replace("http://opentosca.org/self-service", "http://www.eclipse.org/winery/model/selfservice");
                                    newContent = newContent.replace(":application", ":Application");
                                    if (!oldContent.equals(newContent)) {
                                        // we replaced something -> write new content to old file
                                        org.apache.commons.io.FileUtils.writeStringToFile(file.toFile(), newContent, "UTF-8");
                                    }
                                } catch (IOException e) {
                                    CsarImporter.LOGGER.debug("Could not replace content in data.xml", e);
                                }
                            }
                            importFile(file, ref, tmf, rootPath, errors);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    CsarImporter.LOGGER.debug(e.getMessage(), e);
                    errors.add("Self-service Meta Data: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Recursively imports the given definitions
     *
     * @param tmf     the TOSCAMetaFile object holding the parsed content of a TOSCA meta file. If null, no files must
     *                be referenced from the given definitions
     * @param options the set of options applicable while importing a CSAR
     */
    public Optional<ServiceTemplateId> importDefinitions(TOSCAMetaFile tmf, Path definitionsPath, final List<String> errors,
                                                         CsarImportOptions options) throws IOException {
        if (definitionsPath == null) {
            throw new IllegalStateException("path to definitions must not be null");
        }
        if (!Files.exists(definitionsPath)) {
            errors.add(String.format("Definitions %1$s does not exist", definitionsPath.getFileName()));
            return Optional.empty();
        }

        Optional<TDefinitions> parsingResult = parseDefinitionsElement(definitionsPath, errors);

        if (parsingResult.isPresent()) {
            TDefinitions defs = parsingResult.get();
            return processDefinitionsImport(defs, tmf, definitionsPath, errors, options);
        }

        return Optional.empty();
    }

    protected Optional<ServiceTemplateId> processDefinitionsImport(TDefinitions defs, TOSCAMetaFile tmf, Path definitionsPath, List<String> errors, CsarImportOptions options) throws IOException {
        List<TImport> imports = defs.getImport();
        this.importImports(definitionsPath.getParent(), tmf, imports, errors, options);
        // imports has been modified to contain necessary imports only

        // this method adds new imports to defs which may not be imported using "importImports".
        // Therefore, "importTypes" has to be called *after* importImports
        this.importTypes(defs, errors);

        Optional<ServiceTemplateId> entryServiceTemplate = Optional.empty();

        String defaultNamespace = defs.getTargetNamespace();
        List<TExtensibleElements> componentInstanceList = defs.getServiceTemplateOrNodeTypeOrNodeTypeImplementation();
        for (final TExtensibleElements ci : componentInstanceList) {
            // Determine namespace
            String namespace = this.getNamespace(ci, defaultNamespace);
            // Ensure that element has the namespace
            this.setNamespace(ci, namespace);
            // Determine id
            String id = ModelUtilities.getId(ci);

            final DefinitionsChildId wid = determineWineryId(ci, namespace, id);

            if (targetRepository.exists(wid)) {
                if (options.isOverwrite()) {
                    targetRepository.forceDelete(wid);
                    String msg = String.format("Deleted %1$s %2$s to enable replacement", ci.getClass().getName(), wid.getQName().toString());
                    CsarImporter.LOGGER.debug(msg);
                } else {
                    String msg = String.format("Skipped %1$s %2$s, because it already exists", ci.getClass().getName(), wid.getQName().toString());
                    CsarImporter.LOGGER.debug(msg);
                    // this is not displayed in the UI as we currently do not distinguish between pre-existing types and types created during the import.
                    continue;
                }
            }

            // Create a fresh definitions object without the other data.
            final TDefinitions newDefs = BackendUtils.createWrapperDefinitions(wid, targetRepository);

            // copy over the inputs determined by this.importImports
            newDefs.getImport().addAll(imports);

            // add the current TExtensibleElements as the only content to it
            newDefs.getServiceTemplateOrNodeTypeOrNodeTypeImplementation().add(ci);

            if (ci instanceof TArtifactTemplate) {
                // convention: Definitions are stored in the "Definitions" directory, therefore going to levels up (Definitions dir -> root dir) resolves to the root dir
                // COS01, line 2663 states that the path has to be resolved from the *root* of the CSAR
                this.adjustArtifactTemplate(definitionsPath.getParent().getParent(), tmf, (ArtifactTemplateId) wid, (TArtifactTemplate) ci, errors);
            } else if (ci instanceof TNodeType) {
                this.adjustNodeType(definitionsPath.getParent().getParent(), (TNodeType) ci, (NodeTypeId) wid, tmf, errors);
            } else if (ci instanceof TRelationshipType) {
                this.adjustRelationshipType(definitionsPath.getParent().getParent(), (TRelationshipType) ci, (RelationshipTypeId) wid, tmf, errors);
            } else if (ci instanceof TServiceTemplate) {
                this.adjustServiceTemplate(definitionsPath.getParent().getParent(), tmf, (ServiceTemplateId) wid, (TServiceTemplate) ci, errors);
                entryServiceTemplate = Optional.of((ServiceTemplateId) wid);
            }

            // import license and readme files
            importLicenseAndReadme(definitionsPath.getParent().getParent(), wid, tmf, errors);

            // node types and relationship types are subclasses of TEntityType
            // Therefore, we check the entity type separately here
            if (ci instanceof TEntityType) {
                if (options.isAsyncWPDParsing()) {
                    // Adjusting takes a long time
                    // Therefore, we first save the type as is and convert to Winery-Property-Definitions in the background
                    CsarImporter.storeDefinitions(targetRepository, wid, newDefs);
                    CsarImporter.entityTypeAdjustmentService.submit(() -> {
                        adjustEntityType((TEntityType) ci, (EntityTypeId) wid, newDefs, errors);
                        CsarImporter.storeDefinitions(targetRepository, wid, newDefs);
                    });
                } else {
                    adjustEntityType((TEntityType) ci, (EntityTypeId) wid, newDefs, errors);
                    CsarImporter.storeDefinitions(targetRepository, wid, newDefs);
                }
            } else {
                CsarImporter.storeDefinitions(targetRepository, wid, newDefs);
            }
        }

        return entryServiceTemplate;
    }

    protected Optional<TDefinitions> parseDefinitionsElement(Path entryDefinitionsPath, final List<String> errors) {
        Unmarshaller um = JAXBSupport.createUnmarshaller();
        try {
            return Optional.of((TDefinitions) um.unmarshal(entryDefinitionsPath.toFile()));
        } catch (JAXBException e) {
            errors.add("Could not unmarshal definitions " + entryDefinitionsPath.getFileName() + " " + e.getMessage());
            CsarImporter.LOGGER.debug("Unmarshalling error", e);
            return Optional.empty();
        } catch (ClassCastException e) {
            errors.add("Definitions " + entryDefinitionsPath.getFileName() + " is not a TDefinitions " + e.getMessage());
            return Optional.empty();
        }
    }

    protected DefinitionsChildId determineWineryId(TExtensibleElements ci, String namespace, String id) {
        Class<? extends DefinitionsChildId> widClass = Util.getComponentIdClassForTExtensibleElements(ci.getClass());
        return BackendUtils.getDefinitionsChildId(widClass, namespace, id, false);
    }

    /**
     * Imports the specified types into the repository. The types are converted to an import statement
     *
     * @param errors Container for error messages
     */
    private void importTypes(TDefinitions defs, final List<String> errors) {
        Types typesContainer = defs.getTypes();
        if (typesContainer != null) {
            List<Object> types = typesContainer.getAny();
            for (Object type : types) {
                if (type instanceof Element) {
                    Element element = (Element) type;

                    // generate id part of ImportId out of definitions' id
                    // we do not use the name as the name has to be URLencoded again and we have issues with the interplay with org.eclipse.winery.common.ids.definitions.imports.GenericImportId.getId(TImport) then.
                    String id = defs.getId();
                    // try to  make the id unique by hashing the "content" of the definition
                    id = id + "-" + Integer.toHexString(element.hashCode());

                    // set importId
                    DefinitionsChildId importId;
                    String ns;
                    if (element.getNamespaceURI().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
                        ns = element.getAttribute("targetNamespace");
                        importId = new XSDImportId(ns, id, false);
                    } else {
                        // Quick hack for non-XML-Schema-definitions
                        ns = "unknown";
                        importId = new GenericImportId(ns, id, false, element.getNamespaceURI());
                    }

                    // Following code is adapted from importOtherImports

                    TDefinitions wrapperDefs = BackendUtils.createWrapperDefinitions(importId, targetRepository);
                    TImport imp = new TImport();
                    String fileName = id + ".xsd";
                    imp.setLocation(fileName);
                    imp.setImportType(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    imp.setNamespace(ns);
                    wrapperDefs.getImport().add(imp);
                    CsarImporter.storeDefinitions(targetRepository, importId, wrapperDefs);

                    // put the file itself to the repo
                    // ref is required to generate fileRef
                    RepositoryFileReference ref = BackendUtils.getRefOfDefinitions(importId);
                    RepositoryFileReference fileRef = new RepositoryFileReference(ref.getParent(), fileName);
                    // convert element to document
                    // QUICK HACK. Alternative: Add new method targetRepository.getOutputStream and transform DOM node to OuptputStream
                    String content = Util.getXMLAsString(element);
                    try {
                        targetRepository.putContentToFile(fileRef, content, MediaTypes.MEDIATYPE_TEXT_XML);
                    } catch (IOException e) {
                        CsarImporter.LOGGER.debug("Could not put XML Schema definition to file " + fileRef.toString(), e);
                        errors.add("Could not put XML Schema definition to file " + fileRef.toString());
                    }

                    // add import to definitions

                    // adapt path - similar to importOtherImport
                    String newLoc = "../" + Util.getUrlPath(fileRef);
                    imp.setLocation(newLoc);
                    defs.getImport().add(imp);
                } else {
                    // This is a known type. Otherwise JAX-B would render it as Element
                    errors.add("There is a Type of class " + type.getClass().toString() + " which is unknown to Winery. The type element is imported as is");
                }
            }
        }
    }

    /**
     * In case plans are provided, the plans are imported into Winery's storage
     *
     * @param rootPath the root path of the extracted csar
     * @param tmf      the TOSCAMetaFile object used to determine the mime type of the plan
     * @param wid      Winery's internal id of the service template
     * @param st       the the service template to be imported {@inheritDoc}
     */
    private void adjustServiceTemplate(Path rootPath, TOSCAMetaFile tmf, ServiceTemplateId wid, TServiceTemplate st, final List<String> errors) {
        List<TPlan> plans = st.getPlans();
        if (plans != null) {
            for (TPlan plan : plans) {
                PlanModelReference refContainer = plan.getPlanModelReference();
                if (refContainer != null) {
                    String ref = refContainer.getReference();
                    if (ref != null) {
                        // URLs are stored encoded -> undo the encoding
                        ref = EncodingUtil.URLdecode(ref);
                        URI refURI;
                        try {
                            refURI = new URI(ref);
                        } catch (URISyntaxException e) {
                            errors.add(String.format("Invalid URI %1$s", ref));
                            continue;
                        }
                        if (refURI.isAbsolute()) {
                            // Points to somewhere external
                            // This is a linked plan
                            // We have to do nothing
                            continue;
                        }
                        Path path = rootPath.resolve(ref);
                        if (!Files.exists(path)) {
                            // possibly, the reference is relative to the Definitions subfolder
                            // COS01 does not make any explicit statement how to resolve the reference here
                            path = rootPath.resolve("Definitions").resolve(ref);
                            if (!Files.exists(path)) {
                                errors.add(String.format("Plan reference %1$s not found", ref));
                                // we quickly remove the reference to reflect the not-found in the data
                                refContainer.setReference(null);
                                continue;
                            }
                        }
                        PlansId plansId = new PlansId(wid);
                        PlanId pid = new PlanId(plansId, new XmlId(plan.getId(), false));
                        if (Files.isDirectory(path)) {
                            errors.add(String.format("Reference %1$s is a directory and Winery currently does not support importing directories", ref));
                            continue;
                        }
                        RepositoryFileReference fref = new RepositoryFileReference(pid, path.getFileName().toString());
                        importFile(path, fref, tmf, rootPath, errors);

                        // file is imported
                        // Adjust the reference
                        refContainer.setReference("../" + Util.getUrlPath(fref));
                    }
                }
            }
        }
    }

    /**
     * Adds a color to the given relationship type
     */
    void adjustRelationshipType(Path rootPath, TRelationshipType ci, RelationshipTypeId wid, TOSCAMetaFile tmf, final List<String> errors) {
        VisualAppearanceId visId = new VisualAppearanceId(wid);
        this.importIcons(rootPath, visId, tmf, errors);
    }

    void adjustNodeType(Path rootPath, TNodeType ci, NodeTypeId wid, TOSCAMetaFile tmf, final List<String> errors) {
        VisualAppearanceId visId = new VisualAppearanceId(wid);
        this.importIcons(rootPath, visId, tmf, errors);
    }

    protected void importIcons(Path rootPath, VisualAppearanceId visId, TOSCAMetaFile tmf, final List<String> errors) {
        String pathInsideRepo = Util.getPathInsideRepo(visId);
        Path visPath = rootPath.resolve(pathInsideRepo);
        this.importIcon(visId, visPath, Filename.FILENAME_BIG_ICON, tmf, rootPath, errors);
    }

    void importIcon(VisualAppearanceId visId, Path visPath, String fileName, TOSCAMetaFile tmf, Path rootPath, final List<String> errors) {
        Path file = visPath.resolve(fileName);
        if (Files.exists(file)) {
            RepositoryFileReference ref = new RepositoryFileReference(visId, fileName);
            importFile(file, ref, tmf, rootPath, errors);
        }
    }

    void importLicenseAndReadme(Path rootPath, DefinitionsChildId wid, TOSCAMetaFile tmf, final List<String> errors) {
        String pathInsideRepo = Util.getPathInsideRepo(wid);
        Path defPath = rootPath.resolve(pathInsideRepo);
        Path readmeFile = defPath.resolve(Constants.README_FILE_NAME);
        Path licenseFile = defPath.resolve(Constants.LICENSE_FILE_NAME);
        if (Files.exists(readmeFile)) {
            RepositoryFileReference ref = new RepositoryFileReference(wid, Constants.README_FILE_NAME);
            importFile(readmeFile, ref, tmf, rootPath, errors);
        }
        if (Files.exists(licenseFile)) {
            RepositoryFileReference ref = new RepositoryFileReference(wid, Constants.LICENSE_FILE_NAME);
            importFile(licenseFile, ref, tmf, rootPath, errors);
        }
    }

    /**
     * Adjusts the given artifact template to conform with the repository format
     * <p>
     * We import the files given at the artifact references
     */
    private void adjustArtifactTemplate(Path rootPath, TOSCAMetaFile tmf, ArtifactTemplateId atid, TArtifactTemplate ci, final List<String> errors) {
        List<TArtifactReference> refs = ci.getArtifactReferences();
        if (refs == null) {
            // no references stored - break
            return;
        }

        for (TArtifactReference ref : refs) {
            String reference = ref.getReference();
            // URLs are stored encoded -> undo the encoding
            reference = EncodingUtil.URLdecode(reference);

            URI refURI;
            try {
                refURI = new URI(reference);
            } catch (URISyntaxException e) {
                errors.add(String.format("Invalid URI %1$s", ref));
                continue;
            }
            if (refURI.isAbsolute()) {
                // Points to somewhere external
                // We have to do nothing
                continue;
            }

            Path path = rootPath.resolve(reference);
            if (!Files.exists(path)) {
                errors.add(String.format("Reference %1$s not found", reference));
                return;
            }
            Set<Path> allFiles;
            if (Files.isRegularFile(path)) {
                allFiles = new HashSet<>();
                allFiles.add(path);
            } else {
                if (!Files.isDirectory(path)) {
                    LOGGER.error("path {} is not a directory", path);
                }
                Path localRoot = rootPath.resolve(path);
                List<TArtifactReference.IncludeOrExclude> includeOrExclude = ref.getIncludeOrExclude();

                if (includeOrExclude.get(0) instanceof Exclude) {
                    // Implicit semantics of an exclude listed first:
                    // include all files and then exclude the files matched by the pattern
                    allFiles = this.getAllFiles(localRoot);
                } else {
                    // semantics if include listed as first:
                    // same as listed at other places
                    allFiles = new HashSet<>();
                }

                for (Object object : includeOrExclude) {
                    if (object instanceof Include) {
                        this.handleInclude((Include) object, localRoot, allFiles);
                    } else {
                        assert (object instanceof Exclude);
                        this.handleExclude((Exclude) object, localRoot, allFiles);
                    }
                }
            }
            DirectoryId fileDir = new ArtifactTemplateFilesDirectoryId(atid);
            this.importAllFiles(rootPath, allFiles, fileDir, tmf, errors);
        }

        if (refs.isEmpty()) {
            // everything is imported and is a file stored locally
            // we don't need the references stored locally: they are generated on the fly when exporting
            ci.setArtifactReferences(null);
        }
    }

    /**
     * Imports all given files from the file system to the repository
     *
     * @param rootPath    used to make the path p relative in order to determine the mime type and the
     *                    RepositoryFileReference
     * @param files       list of all files
     * @param directoryId the id of the directory of the artifact template the files to attach to
     * @param tmf         the TOSCAMetaFile object used to determine the mimetype. Must not be null.
     * @param errors      list where import errors should be stored to
     */
    protected void importAllFiles(Path rootPath, Collection<Path> files, DirectoryId directoryId, TOSCAMetaFile tmf, final List<String> errors) {
        // remove the filePathInsideRepo to correctly store the files in the files folder inside an artifact template
        // otherwise, the files are saved in the sub directory of the artifact template
        // this is required, to enable the cycle CSAR export, clean , import CSAR
        String pathInsideRepo = Util.getPathInsideRepo(directoryId);

        for (Path p : files) {
            if (!Files.exists(p)) {
                errors.add(String.format("File %1$s does not exist", p));
                return;
            }
            // directoryId already identifies the subdirectory
            RepositoryFileReference fileReference = new RepositoryFileReference(directoryId, p.getFileName().toString());
            importFile(p, fileReference, tmf, rootPath, errors);
        }
    }

    /**
     * Modifies given allFiles object to exclude all files given by the excl pattern
     * <p>
     * Semantics: Remove all files from the set, which match the given pattern
     */
    private void handleExclude(Exclude excl, Path localRoot, Set<Path> allFiles) {
        PathMatcher pathMatcher = localRoot.getFileSystem().getPathMatcher("glob:" + excl.getPattern());
        allFiles.removeIf(pathMatcher::matches);
    }

    /**
     * Modifies given allFiles object to include all files given by the incl pattern
     * <p>
     * Semantics: Add all files from localRoot to allFiles matching the pattern
     */
    private void handleInclude(final Include incl, final Path localRoot, final Set<Path> allFiles) {
        final PathMatcher pathMatcher = localRoot.getFileSystem().getPathMatcher("glob:" + incl.getPattern());
        try {
            Files.walkFileTree(localRoot, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relFile = localRoot.relativize(file);
                    if (pathMatcher.matches(relFile)) {
                        allFiles.add(file);
                    }
                    return CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (pathMatcher.matches(dir)) {
                        Set<Path> filesToAdd = CsarImporter.this.getAllFiles(dir);
                        allFiles.addAll(filesToAdd);
                        return SKIP_SUBTREE;
                    } else {
                        return CONTINUE;
                    }
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Lists all files contained in the given path
     */
    private Set<Path> getAllFiles(Path startPath) {
        final Set<Path> res = new HashSet<>();
        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    res.add(file);
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return res;
    }

    /**
     * Sets the namespace on the CI if CI offers the method "setTargetNamespace"
     *
     * @param ci        the component instance to set the namespace
     * @param namespace the namespace to set
     */
    public void setNamespace(TExtensibleElements ci, String namespace) {
        Method method;
        try {
            method = ci.getClass().getMethod("setTargetNamespace", String.class);
            method.invoke(ci, namespace);
        } catch (NoSuchMethodException ne) {
            // this is OK, because we do not check, whether the method really exists
            // Special case for TArtifactTemplate not offering setTargetNamespace
            // just ignore it
        } catch (Exception e) {
            throw new IllegalStateException("Could not set target namespace", e);
        }
    }

    /**
     * @param ci               the component instance to get the namespace from
     * @param defaultNamespace the namespace to use if the TExtensibleElements has no targetNamespace
     */
    public String getNamespace(TExtensibleElements ci, String defaultNamespace) {
        Method method;
        Object res;
        try {
            method = ci.getClass().getMethod("getTargetNamespace");
            res = method.invoke(ci);
        } catch (Exception e) {
            // we are at TArtifactTemplate, which does not offer getTargetNamespace
            res = null;
        }
        String ns = (String) res;
        if (ns == null) {
            ns = defaultNamespace;
        }
        return ns;
    }

    /**
     * @param basePath the base path where to resolve files from. This is the directory of the Definitions
     * @param imports  the list of imports to import. SIDE EFFECT: this list is modified. After this method has run, the
     *                 list contains the imports to be put into the wrapper element
     * @param options  the set of options applicable while importing a CSAR
     */
    private void importImports(Path basePath, TOSCAMetaFile tmf, List<TImport> imports, final List<String> errors, CsarImportOptions options) throws IOException {
        for (Iterator<TImport> iterator = imports.iterator(); iterator.hasNext(); ) {
            TImport imp = iterator.next();
            String importType = imp.getImportType();
            String namespace = imp.getNamespace();
            String loc = imp.getLocation();

            if (namespace == null) {
                errors.add("not namespace-qualified imports are not supported.");
                continue;
            }

            if (loc == null) {
                errors.add("Empty location imports are not supported.");
            } else {
                if (importType.equals(Namespaces.TOSCA_NAMESPACE)) {
                    if (!Util.isRelativeURI(loc)) {
                        errors.add("Absolute URIs for definitions import not supported.");
                        continue;
                    }

                    // URIs are encoded
                    loc = EncodingUtil.URLdecode(loc);

                    Path defsPath = basePath.resolve(loc);
                    // fallback for older CSARs, where the location is given from the root
                    if (!Files.exists(defsPath)) {
                        defsPath = basePath.getParent().resolve(loc);
                        // the real existence check is done in importDefinitions
                    }
                    this.importDefinitions(tmf, defsPath, errors, options);
                    // imports of definitions don't have to be kept as these are managed by Winery
                    iterator.remove();
                } else {
                    this.importOtherImport(basePath, imp, errors, importType, options);
                }
            }
        }
    }

    /**
     * SIDE EFFECT: modifies the location of imp to point to the correct relative location (when read from the exported
     * CSAR)
     *
     * @param rootPath the absolute path where to resolve files from
     * @param options  the set of options applicable while importing a CSAR
     */
    private void importOtherImport(Path rootPath, TImport imp, final List<String> errors, String type, CsarImportOptions options) {
        assert (!type.equals(Namespaces.TOSCA_NAMESPACE));
        String loc = imp.getLocation();

        if (!Util.isRelativeURI(loc)) {
            // This is just an information message
            errors.add("Absolute URIs are not resolved by Winery (" + loc + ")");
            return;
        }

        // location URLs are encoded: http://www.w3.org/TR/2001/WD-charmod-20010126/#sec-URIs, RFC http://www.ietf.org/rfc/rfc2396.txt
        loc = EncodingUtil.URLdecode(loc);
        Path path;
        try {
            path = rootPath.resolve(loc);
        } catch (Exception e) {
            // java.nio.file.InvalidPathException could be thrown which is a RuntimeException
            errors.add(e.getMessage());
            return;
        }
        if (!Files.exists(path)) {
            // fallback for older CSARs, where the location is given from the root
            path = rootPath.getParent().resolve(loc);
            if (!Files.exists(path)) {
                errors.add(String.format("File %1$s does not exist", loc));
                return;
            }
        }
        String namespace = imp.getNamespace();
        String fileName = path.getFileName().toString();
        String id = fileName;
        id = FilenameUtils.removeExtension(id);
        // Convention: id of import is filename without extension

        GenericImportId rid;
        if (type.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            rid = new XSDImportId(namespace, id, false);
        } else {
            rid = new GenericImportId(namespace, id, false, type);
        }

        boolean importDataExistsInRepo = targetRepository.exists(rid);

        if (!importDataExistsInRepo) {
            // We have to
            //  a) create a .definitions file
            //  b) put the file itself in the repo

            // Create the definitions file
            TDefinitions defs = BackendUtils.createWrapperDefinitions(rid, targetRepository);
            defs.getImport().add(imp);
            // QUICK HACK: We change the imp object's location here and below again
            // This is "OK" as "storeDefinitions" serializes the current state and not the future state of the imp object
            // change the location to point to the file in the folder of the .definitions file
            imp.setLocation(fileName);

            // put the definitions file to the repository
            CsarImporter.storeDefinitions(targetRepository, rid, defs);
        }

        // put the file itself to the repo
        // ref is required to generate fileRef
        RepositoryFileReference ref = BackendUtils.getRefOfDefinitions(rid);
        RepositoryFileReference fileRef = new RepositoryFileReference(ref.getParent(), fileName);

        // location is relative to Definitions/
        // even if the import already exists, we have to adapt the path
        // URIs are encoded
        String newLoc = "../" + Util.getUrlPath(fileRef);
        imp.setLocation(newLoc);

        if (!importDataExistsInRepo || options.isOverwrite()) {
            // finally write the file to the storage
            try (InputStream is = Files.newInputStream(path);
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                MediaType mediaType;
                if (type.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
                    mediaType = MediaTypes.MEDIATYPE_XSD;
                } else {
                    mediaType = BackendUtils.getMimeType(bis, path.getFileName().toString());
                }
                targetRepository.putContentToFile(fileRef, bis, mediaType);
            } catch (IllegalArgumentException | IOException e) {
                throw new IllegalStateException(e);
            }

            // we have to update the cache in case of a new XSD to speedup usage of winery
            if (rid instanceof XSDImportId) {
                // We do the initialization asynchronously
                // We do not check whether the XSD has already been checked
                // We cannot just check whether an XSD already has been handled since the XSD could change over time
                // Synchronization at org.eclipse.winery.repository.resources.imports.xsdimports.XSDImportResource.getAllDefinedLocalNames(short) also isn't feasible as the backend doesn't support locks
                CsarImporter.xsdParsingService.submit(() -> {
                    CsarImporter.LOGGER.debug("Updating XSD import cache data");
                    // We call the queries without storing the result:
                    // We use the SIDEEFFECT that a cache is created
                    final XsdImportManager xsdImportManager = targetRepository.getXsdImportManager();
                    xsdImportManager.getAllDeclaredElementsLocalNames();
                    xsdImportManager.getAllDefinedTypesLocalNames();
                    CsarImporter.LOGGER.debug("Updated XSD import cache data");
                });
            }
        }
    }
}
