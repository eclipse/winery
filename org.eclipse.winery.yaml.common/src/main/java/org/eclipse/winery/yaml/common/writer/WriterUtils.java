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
 *******************************************************************************/
package org.eclipse.winery.yaml.common.writer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.imports.GenericImportId;
import org.eclipse.winery.common.ids.definitions.imports.XSDImportId;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.constants.MediaTypes;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateFilesDirectoryId;
import org.eclipse.winery.repository.importing.CsarImporter;
import org.eclipse.winery.yaml.common.Namespaces;
import org.eclipse.winery.yaml.common.reader.xml.Reader;
import org.eclipse.winery.yaml.common.writer.xml.Writer;

import org.apache.tika.mime.MediaType;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class WriterUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriterUtils.class);

    public static Definitions storeDefinitions(Definitions definitions, boolean overwrite, Path dir) {
        Path path = null;
        try {
            path = Files.createTempDirectory("winery");
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.debug("Store definition: {}", definitions.getId());
        saveDefinitions(definitions, path, definitions.getTargetNamespace(), definitions.getId());
        Definitions cleanDefinitions = loadDefinitions(path, definitions.getTargetNamespace(), definitions.getId());

        CsarImporter csarImporter = new CsarImporter();
        List<Exception> exceptions = new ArrayList<>();
        cleanDefinitions.getServiceTemplateOrNodeTypeOrNodeTypeImplementation().forEach(entry -> {
            String namespace = csarImporter.getNamespace(entry, definitions.getTargetNamespace());
            csarImporter.setNamespace(entry, namespace);

            String id = ModelUtilities.getId(entry);

            Class<? extends DefinitionsChildId> widClazz = Util.getComponentIdClassForTExtensibleElements(entry.getClass());
            final DefinitionsChildId wid = BackendUtils.getDefinitionsChildId(widClazz, namespace, id, false);

            if (RepositoryFactory.getRepository().exists(wid)) {
                if (overwrite) {
                    try {
                        RepositoryFactory.getRepository().forceDelete(wid);
                    } catch (IOException e) {
                        exceptions.add(e);
                    }
                } else {
                    return;
                }
            }

            if (entry instanceof TArtifactTemplate) {
                TArtifactTemplate.ArtifactReferences artifactReferences = ((TArtifactTemplate) entry).getArtifactReferences();
                Stream.of(artifactReferences)
                    .filter(Objects::nonNull)
                    .flatMap(ref -> ref.getArtifactReference().stream())
                    .filter(Objects::nonNull)
                    .forEach(ref -> {
                        String reference = ref.getReference();
                        URI refURI;
                        try {
                            refURI = new URI(reference);
                        } catch (URISyntaxException e) {
                            LOGGER.error("Invalid URI {}", reference);
                            return;
                        }
                        if (refURI.isAbsolute()) {
                            return;
                        }
                        Path artifactPath = dir.resolve(reference);
                        if (!Files.exists(artifactPath)) {
                            LOGGER.error("File not found {}", artifactPath);
                            return;
                        }
                        ArtifactTemplateFilesDirectoryId aDir = new ArtifactTemplateFilesDirectoryId((ArtifactTemplateId) wid);
                        RepositoryFileReference aFile = new RepositoryFileReference(aDir, artifactPath.getFileName().toString());
                        MediaType mediaType = null;
                        try (InputStream is = Files.newInputStream(artifactPath);
                             BufferedInputStream bis = new BufferedInputStream(is)) {
                            mediaType = BackendUtils.getMimeType(bis, artifactPath.getFileName().toString());
                            RepositoryFactory.getRepository().putContentToFile(aFile, bis, mediaType);
                        } catch (IOException e) {
                            LOGGER.error("Could not read artifact template file: {}", artifactPath);
                            return;
                        }
                    });
            }

            final Definitions part = BackendUtils.createWrapperDefinitions(wid);
            part.getServiceTemplateOrNodeTypeOrNodeTypeImplementation().add(entry);

            RepositoryFileReference ref = BackendUtils.getRefOfDefinitions(wid);
            String content = BackendUtils.getXMLAsString(part, true);
            try {
                RepositoryFactory.getRepository().putContentToFile(ref, content, MediaTypes.MEDIATYPE_TOSCA_DEFINITIONS);
            } catch (Exception e) {
                exceptions.add(e);
            }
        });

        return cleanDefinitions;
    }

    public static void storeTypes(Path path, String name, String namespace, String id) {
        WriterUtils.storeTypes(getTypeFile(path, namespace, name).getParent(), namespace, id);
    }

    public static void storeTypes(Path path, String namespace, String id) {
        LOGGER.debug("Store type: {}", id);
        try {
            MediaType mediaType = MediaTypes.MEDIATYPE_XSD;

            TImport.Builder builder = new TImport.Builder(Namespaces.XML_NS);
            builder.setNamespace(namespace);
            builder.setLocation(id + ".xsd");

            GenericImportId rid = new XSDImportId(namespace, id, false);
            TDefinitions definitions = BackendUtils.createWrapperDefinitions(rid);
            definitions.getImport().add(builder.build());
            CsarImporter.storeDefinitions(rid, definitions);

            RepositoryFileReference ref = BackendUtils.getRefOfDefinitions(rid);

            List<File> files = Files.list(path).filter(Files::isRegularFile)
                .map(Path::toFile).collect(Collectors.toList());
            for (File file : files) {
                BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
                RepositoryFileReference fileRef = new RepositoryFileReference(ref.getParent(), file.getName());
                RepositoryFactory.getRepository().putContentToFile(fileRef, stream, mediaType);
            }
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void saveDefinitions(Definitions definitions, Path path, String namespace, String name) {
        saveDefinitions(definitions, getDefinitionsPath(path, namespace, name));
    }

    public static void saveDefinitions(Definitions definitions, Path filePath) {
        Writer writer = new Writer();
        try {
            writer.writeXML(definitions, filePath);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static Definitions loadDefinitions(Path path, String namespace, String name) {
        Path filePath = getDefinitionsPath(path, namespace, name);
        Reader reader = new Reader();
        try {
            return reader.parse(new FileInputStream(filePath.toFile()));
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }
        return new Definitions();
    }

    public static void saveType(Document document, Path path, String namespace, String name) {
        WriterUtils.saveType(document, getTypeFile(path, namespace, name));
    }

    public static void saveType(Document document, Path filePath) {
        DOMSource source = new DOMSource(document);
        try {
            Files.createDirectories(filePath.getParent());
            FileWriter writer = new FileWriter(filePath.toFile());
            StreamResult result = new StreamResult(writer);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            writer.flush();
            writer.close();
        } catch (TransformerException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param path The root path to search from
     * @param namespace the namespace
     * @param name the local name of the file
     */
    private static Path getTypeFile(@NonNull Path path, @NonNull String namespace, @NonNull String name) {
        String urlEncodedName = Util.URLencode(name);
        return path.resolve(urlEncodedName)
            .resolve(Util.URLencode(namespace))
            .resolve("types")
            .resolve(urlEncodedName.concat(".xsd"));
    }

    private static Path getDefinitionsPath(Path path, String namespace, @NonNull String name) {
        return path.resolve(Util.URLencode(namespace))
            .resolve(name.concat(".tosca"));
    }

    public static String getDefinitionsLocation(String namespace, @NonNull String name) {
        return Util.URLencode(namespace) + File.separator
            + Util.URLencode(name) + ".tosca";
    }
}
    
