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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.winery.common.Constants;
import org.eclipse.winery.common.configuration.Environments;
import org.eclipse.winery.common.constants.MimeTypes;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.datatypes.ids.elements.DirectoryId;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.CsarContentProperties;
import org.eclipse.winery.repository.export.CsarExporter;
import org.eclipse.winery.repository.export.ExportedState;
import org.eclipse.winery.repository.export.entries.CsarEntry;
import org.eclipse.winery.repository.export.entries.DefinitionsBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.DocumentBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.RemoteRefBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.RepositoryRefBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.XMLDefinitionsBasedCsarEntry;
import org.eclipse.winery.repository.yaml.YamlRepository;
import org.eclipse.winery.repository.yaml.export.entries.YAMLDefinitionsBasedCsarEntry;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.CONTENT_TYPE;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.CREATED_BY;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.CSAR_VERSION;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.CSAR_VERSION_VALUE_FOR_YAML;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.ENTRY_DEFINITIONS;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.NAME;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_META_FILE_VERSION;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_META_FILE_VERSION_VALUE;

public class YamlExporter extends CsarExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlExporter.class);
    private static final String DEFINITIONS_PATH_PREFIX = "_definitions/";

    private final IRepository repository;

    public YamlExporter(IRepository repository) {
        super(repository);
        this.repository = repository;
    }

    /**
     * Returns a unique name for the given definitions to be used as filename
     */
    public static String getDefinitionsName(IRepository repository, DefinitionsChildId id) {
        // the prefix is globally unique and the id locally in a namespace
        // therefore a concatenation of both is also unique
        return repository.getNamespaceManager().getPrefix(id.getNamespace()) + "__" + id.getXmlId().getEncoded();
    }

    public static String getDefinitionsPathInsideCSAR(IRepository repository, DefinitionsChildId id) {
        return DEFINITIONS_PATH_PREFIX
            .concat(getDefinitionsName(repository, id))
            .concat(Constants.SUFFIX_TOSCA_DEFINITIONS);
    }

    /**
     * Writes a complete CSAR containing all necessary things reachable from the given service template
     *
     * @param entryId the id of the service template to export
     * @param out     the output stream to write to
     * @return the TOSCA meta file for the generated Csar
     */
    @Override
    public String writeCsar(DefinitionsChildId entryId,
                            OutputStream out, Map<String, Object> exportConfiguration) throws IOException, RepositoryCorruptException {
        LOGGER.trace("Starting CSAR export with {}", entryId.toString());

        Map<CsarContentProperties, CsarEntry> refMap = new HashMap<>();
        YamlToscaExportUtil exporter = new YamlToscaExportUtil(Environments.getInstance().getRepositoryConfig().hasExportNormativeTypes());        
        ExportedState exportedState = new ExportedState();
        DefinitionsChildId currentId = entryId;
        Collection<DefinitionsChildId> referencedIds;

        // Process definitions and referenced files
        do {
            String definitionsPathInsideCSAR = getDefinitionsPathInsideCSAR(repository, currentId);
            CsarContentProperties definitionsFileProperties = new CsarContentProperties(definitionsPathInsideCSAR);
            if (!YamlRepository.ROOT_TYPE_QNAME.equals(currentId.getQName())) {
                referencedIds = exporter.processTOSCA(repository, currentId, definitionsFileProperties, refMap, exportConfiguration);
                // for each entryId add license and readme files (if they exist) to the refMap
                addLicenseAndReadmeFiles(currentId, refMap);

                exportedState.flagAsExported(currentId);
                exportedState.flagAsExportRequired(referencedIds);
            }

            currentId = exportedState.pop();
        } while (currentId != null);

        // Archive creation
        try (final ZipOutputStream zos = new ZipOutputStream(out)) {
            // write all referenced files
            for (Map.Entry<CsarContentProperties, CsarEntry> entry : refMap.entrySet()) {
                CsarContentProperties fileProperties = entry.getKey();
                CsarEntry ref = entry.getValue();
                LOGGER.trace("Creating {}", fileProperties.getPathInsideCsar());

                if (ref instanceof RepositoryRefBasedCsarEntry && ((RepositoryRefBasedCsarEntry) ref).getReference().getParent() instanceof DirectoryId) {
                    addArtifactTemplateToZipFile(zos, (RepositoryRefBasedCsarEntry) ref, fileProperties);
                } else {
                    addCsarEntryToArchive(zos, ref, fileProperties);
                }
            }

            // create manifest file and add it to archive
            return this.addManifest(repository, entryId, refMap, zos, exportConfiguration);
        }
    }

    /**
     * Adds a file to an archive
     *
     * @param zos                   Output stream of the archive
     * @param csarEntry             Reference to the file that should be added to the archive
     * @param csarContentProperties Describing the path inside the archive to the file
     */
    private void addCsarEntryToArchive(ZipOutputStream zos, CsarEntry csarEntry,
                                       CsarContentProperties csarContentProperties) {
        try (InputStream is = csarEntry.getInputStream()) {
            zos.putNextEntry(new ZipEntry(csarContentProperties.getPathInsideCsar()));
            IOUtils.copy(is, zos);
            zos.closeEntry();
        } catch (Exception e) {
            LOGGER.error("Could not copy file content to ZIP outputstream", e);
        }
    }

    private String addManifest(IRepository repository, DefinitionsChildId id, Map<CsarContentProperties, CsarEntry> refMap,
                               ZipOutputStream out, Map<String, Object> exportConfiguration) throws IOException {
        String entryDefinitionsReference = getDefinitionsPathInsideCSAR(repository, id);

        out.putNextEntry(new ZipEntry("TOSCA-Metadata/TOSCA.meta"));
        StringBuilder stringBuilder = new StringBuilder();

        // Setting Versions
        stringBuilder.append(TOSCA_META_FILE_VERSION).append(": ").append(TOSCA_META_FILE_VERSION_VALUE).append("\n");
        stringBuilder.append(CSAR_VERSION).append(": ").append(CSAR_VERSION_VALUE_FOR_YAML).append("\n");
        stringBuilder.append(CREATED_BY).append(": Winery ").append(Environments.getInstance().getVersion()).append("\n");

        // Winery currently is unaware of tDefinitions, therefore, we use the
        // name of the service template
        stringBuilder.append(ENTRY_DEFINITIONS).append(": ").append(entryDefinitionsReference).append("\n");
        stringBuilder.append("\n");

        assert (refMap.keySet()
            .stream()
            .anyMatch(
                fileProperties -> fileProperties
                    .getPathInsideCsar()
                    .equals(entryDefinitionsReference)
            )
        );

        // Setting other files, mainly files belonging to artifacts
        for (Map.Entry<CsarContentProperties, CsarEntry> item : refMap.entrySet()) {
            final CsarEntry csarEntry = item.getValue();
            final CsarContentProperties fileProperties = item.getKey();

            stringBuilder.append(NAME).append(": ").append(fileProperties.getPathInsideCsar()).append("\n");

            String mimeType;

            if (csarEntry instanceof DocumentBasedCsarEntry) {
                mimeType = MimeTypes.MIMETYPE_XSD;
            } else if (csarEntry instanceof XMLDefinitionsBasedCsarEntry ||
                csarEntry instanceof YAMLDefinitionsBasedCsarEntry ||
                csarEntry instanceof DefinitionsBasedCsarEntry) {
                mimeType = MimeTypes.MIMETYPE_TOSCA_DEFINITIONS;
            } else if (csarEntry instanceof RemoteRefBasedCsarEntry) {
                mimeType = repository.getMimeType((RemoteRefBasedCsarEntry) csarEntry);
            } else {
                mimeType = repository.getMimeType(((RepositoryRefBasedCsarEntry) csarEntry).getReference());
            }

            stringBuilder.append(CONTENT_TYPE).append(": ").append(mimeType).append("\n");
            stringBuilder.append("\n");
        }

        String manifestString = stringBuilder.toString();
        out.write(manifestString.getBytes());
        out.closeEntry();

        return manifestString;
    }
}
