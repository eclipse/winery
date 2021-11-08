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
package org.eclipse.winery.repository.export;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.ids.EncodingUtil;
import org.eclipse.winery.model.ids.IdUtil;
import org.eclipse.winery.model.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.ids.definitions.EntityTypeId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.NodeTypeImplementationId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.ids.definitions.TopologyGraphElementEntityTypeId;
import org.eclipse.winery.model.ids.definitions.imports.GenericImportId;
import org.eclipse.winery.model.ids.elements.PlanId;
import org.eclipse.winery.model.ids.elements.PlansId;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.model.tosca.constants.QNames;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.constants.Filename;
import org.eclipse.winery.repository.common.RepositoryFileReference;
import org.eclipse.winery.repository.common.Util;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateFilesDirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.DirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.VisualAppearanceId;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.entries.CsarEntry;
import org.eclipse.winery.repository.export.entries.DefinitionsBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.DocumentBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.RepositoryRefBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.XMLDefinitionsBasedCsarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class ToscaExportUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToscaExportUtil.class);

    /*
     * these two are GLOBAL VARIABLES leading to the fact that this class has to
     * be constructed for each export
     */

    // collects the references to be put in the CSAR and the assigned path in
    // the CSAR MANIFEST
    // this allows to use other paths in the CSAR than on the local storage
    protected Map<CsarContentProperties, CsarEntry> referencesToPathInCSARMap = new HashMap<>();

    /**
     * Currently a very simple approach to configure the export
     */
    protected Map<String, Object> exportConfiguration;

    public void writeTOSCA(IRepository repository, DefinitionsChildId id,
                           Map<String, Object> conf, OutputStream outputStream)
        throws RepositoryCorruptException, IOException {
        CsarEntry csarEntry = getExportableEntry(repository, id, conf);
        csarEntry.writeToOutputStream(outputStream);
    }

    public TDefinitions getExportableDefinitions(IRepository repository, DefinitionsChildId id)
        throws IOException, RepositoryCorruptException {
        if (!repository.exists(id)) {
            String error = String.format("Component instance %s does not exist.", id.toReadableString());
            LOGGER.warn(error);
            return null;
        }
        if (this.exportConfiguration == null) {
            this.exportConfiguration = new HashMap<>();
        }
        this.getPrepareForExport(repository, id);

        Collection<DefinitionsChildId> referencedDefinitionsChildIds = repository.getReferencedDefinitionsChildIds(id);
        return specifyImports(repository, id, referencedDefinitionsChildIds);
    }

    private CsarEntry getExportableEntry(IRepository repository, DefinitionsChildId id, Map<String, Object> conf) throws IOException, RepositoryCorruptException {
        this.processTOSCA(repository, id, new CsarContentProperties(id.getQName().toString()), conf);
        return this.referencesToPathInCSARMap.values().stream()
            // FIXME ... why is this even restricted that way if all we want to do is "writeTOSCA"?
            .filter(e -> e instanceof XMLDefinitionsBasedCsarEntry || e instanceof DefinitionsBasedCsarEntry)
            .findFirst()
            .orElseThrow(() -> new RepositoryCorruptException("Definition not found!"));
    }

    /**
     * Completes the tosca xml in preparation to write it
     *
     * @param id                  the id of the definition child to export
     * @param exportConfiguration the configuration map for the export.
     * @return a collection of DefinitionsChildIds referenced by the given component
     */
    public Collection<DefinitionsChildId> processTOSCA(IRepository repository, DefinitionsChildId id,
                                                       CsarContentProperties definitionsFileProperties,
                                                       Map<String, Object> exportConfiguration) throws IOException, RepositoryCorruptException {
        this.exportConfiguration = exportConfiguration;
        return this.processDefinitionsElement(repository, id, definitionsFileProperties);
    }

    /**
     * Completes the tosca xml in preparation to write it. Additionally, a the artifactMap is filled to enable the CSAR
     * exporter to create necessary entries in TOSCA-Meta and to add them to the CSAR itself
     *
     * @param id                        the component instance to export
     * @param exportConfiguration       Configures the exporter
     * @param referencesToPathInCSARMap collects the references to export. It is updated during the export
     * @return a collection of DefinitionsChildIds referenced by the given component
     */
    // marked as public to allow access to the overridden implementation in YamlToscaExportUtil from YamlExporter
    public Collection<DefinitionsChildId> processTOSCA(IRepository repository, DefinitionsChildId id, CsarContentProperties definitionsFileProperties,
                                                       Map<CsarContentProperties, CsarEntry> referencesToPathInCSARMap,
                                                       Map<String, Object> exportConfiguration) throws IOException, RepositoryCorruptException {
        this.referencesToPathInCSARMap = referencesToPathInCSARMap;
        return this.processTOSCA(repository, id, definitionsFileProperties, exportConfiguration);
    }

    /**
     * Writes the Definitions belonging to the given definition children to the output stream
     *
     * @return a collection of DefinitionsChildIds referenced by the given component
     * @throws RepositoryCorruptException if tcId does not exist
     */
    protected Collection<DefinitionsChildId> processDefinitionsElement(IRepository repository, DefinitionsChildId tcId,
                                                                       CsarContentProperties definitionsFileProperties) throws RepositoryCorruptException, IOException {
        if (!repository.exists(tcId)) {
            String error = "Component instance " + tcId.toReadableString() + " does not exist.";
            ToscaExportUtil.LOGGER.error(error);
            throw new RepositoryCorruptException(error);
        }

        this.getPrepareForExport(repository, tcId);

        // this doesn't need to be exhaustive, because this method is only called for a full export.
        // as such all references are traversed externally
        Collection<DefinitionsChildId> references = repository.getReferencedDefinitionsChildIds(tcId);
        TDefinitions entryDefinitions = specifyImports(repository, tcId, references);

        this.referencesToPathInCSARMap.put(definitionsFileProperties, new DefinitionsBasedCsarEntry(entryDefinitions, repository));

        return references;
    }

    private TDefinitions specifyImports(IRepository repository, DefinitionsChildId tcId, Collection<DefinitionsChildId> referencedDefinitionsChildIds) {
        TDefinitions entryDefinitions = repository.getDefinitions(tcId);

        // BEGIN: Definitions modification
        // the "imports" collection contains the imports of Definitions, not of other definitions
        // the other definitions are stored in entryDefinitions.getImport()
        // we modify the internal definitions object directly. It is not written back to the storage. Therefore, we do not need to clone it

        // the imports (pointing to not-definitions (xsd, wsdl, ...)) already have a correct relative URL. (quick hack)
        URI uri = (URI) this.exportConfiguration.get(CsarExportConfiguration.REPOSITORY_URI.name());
        if (uri != null) {
            // we are in the plain-XML mode, the URLs of the imports have to be adjusted
            for (TImport i : entryDefinitions.getImport()) {
                String loc = i.getLocation();
                if (!loc.startsWith("../")) {
                    LOGGER.warn("Location is not relative for id " + tcId.toReadableString());
                }

                loc = loc.substring(3);
                loc = uri + loc;
                // now the location is an absolute URL
                i.setLocation(loc);
            }
        }

        // files of imports have to be added to the CSAR, too
        for (TImport i : entryDefinitions.getImport()) {
            String loc = i.getLocation();
            if (Util.isRelativeURI(loc)) {
                // locally stored, add to CSAR
                GenericImportId iid = new GenericImportId(i);
                String fileName = IdUtil.getLastURIPart(loc);
                fileName = EncodingUtil.URLdecode(fileName);
                RepositoryFileReference ref = new RepositoryFileReference(iid, fileName);
                putRefAsReferencedItemInCsar(repository, ref);
            }
        }

        Set<DefinitionsChildId> collect = referencedDefinitionsChildIds.stream()
            .filter(id -> id instanceof NodeTypeImplementationId)
            .collect(Collectors.toSet());
        if (collect.stream().anyMatch(DefinitionsChildId::isSelfContained)) {
            if (this.exportConfiguration.containsKey(CsarExportConfiguration.INCLUDE_DEPENDENCIES.name())) {
                referencedDefinitionsChildIds.removeAll(
                    collect.stream()
                        .filter(id -> !id.isSelfContained())
                        .collect(Collectors.toList())
                );
            } else if (collect.size() > 1 && collect.stream().anyMatch(id -> !id.isSelfContained())) {
                referencedDefinitionsChildIds.removeAll(
                    collect.stream()
                        .filter(DefinitionsChildId::isSelfContained)
                        .collect(Collectors.toList())
                );
            }
        }

        // adjust imports: add imports of definitions to it
        Collection<TImport> imports = new ArrayList<>();
        for (DefinitionsChildId id : referencedDefinitionsChildIds) {
            this.addToImports(repository, id, imports);
        }

        entryDefinitions.getImport().addAll(imports);

        // END: Definitions modification
        return entryDefinitions;
    }

    protected void exportEntityType(TDefinitions entryDefinitions, URI uri, DefinitionsChildId tcId) {
        TEntityType entityType = (TEntityType) entryDefinitions.getElement();

        // we have an entity type with a possible properties definition
        WinerysPropertiesDefinition wpd = entityType.getWinerysPropertiesDefinition();
        if (wpd != null) {
            if (wpd.getIsDerivedFromXSD() == null) {
                // Write WPD only to file if it exists and is NOT derived from an XSD (which may happen during import)

                String wrapperElementNamespace = wpd.getNamespace();
                String wrapperElementLocalName = wpd.getElementName();

                // BEGIN: add import and put into CSAR

                TImport imp = new TImport();
                entryDefinitions.getImport().add(imp);

                // fill known import values
                imp.setImportType(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                imp.setNamespace(wrapperElementNamespace);
                // add "winerysPropertiesDefinition" flag to import tag to support
                Map<QName, String> otherAttributes = imp.getOtherAttributes();
                otherAttributes.put(QNames.QNAME_WINERYS_PROPERTIES_DEFINITION_ATTRIBUTE, "true");

                // Determine location
                String loc = BackendUtils.getImportLocationForWinerysPropertiesDefinitionXSD((EntityTypeId) tcId, uri, wrapperElementLocalName);
                if (uri == null) {
                    ToscaExportUtil.LOGGER.trace("CSAR Export mode. Putting XSD into CSAR");
                    // CSAR Export mode
                    // XSD has to be put into the CSAR
                    Document document = ModelUtilities.getWinerysPropertiesDefinitionXsdAsDocument(wpd);

                    // loc in import is URL encoded, loc on filesystem isn't
                    String locInCSAR = EncodingUtil.URLdecode(loc);
                    // furthermore, the path has to start from the root of the CSAR; currently, it starts from Definitions/
                    locInCSAR = locInCSAR.substring(3);
                    ToscaExportUtil.LOGGER.trace("Location in CSAR: {}", locInCSAR);
                    CsarContentProperties csarContentProperties = new CsarContentProperties(locInCSAR);
                    this.referencesToPathInCSARMap.put(csarContentProperties, new DocumentBasedCsarEntry(document));
                }
                imp.setLocation(loc);

                // END: add import and put into CSAR

                // BEGIN: generate TOSCA conforming PropertiesDefinition

                // Winerys properties definitions are serialized as an XSD.
                // As such their TOSCA representation is a type reference, so we generate that here.
                // Of course that breaks when we export as YAML, but this exporter basically targets XML in the first place
                // TODO: future work — deal with WPD when exporting to YAML
                //  (a map[string, string] can be represented as a TDataType)
                TEntityType.XmlTypeDefinition propertiesDefinition = new TEntityType.XmlTypeDefinition();
                propertiesDefinition.setType(new QName(wrapperElementNamespace, wrapperElementLocalName));
                entityType.setProperties(propertiesDefinition);

                // END: generate TOSCA conforming PropertiesDefinition
            } else {
                // otherwise WPD exists, but is derived from XSD
                // we DO NOT have to remove the winery properties definition from the output to allow "debugging" of the CSAR
            }
        }
    }

    /**
     * Prepares the given id for export. Mostly, the contained files are added to the CSAR.
     */
    private void getPrepareForExport(IRepository repository, DefinitionsChildId id) throws IOException {
        // prepareForExport adds the contained files to the CSAR, not the referenced ones.
        // These are added later
        if (id instanceof ServiceTemplateId) {
            this.prepareForExport(repository, (ServiceTemplateId) id);
        } else if (id instanceof RelationshipTypeId) {
            this.addVisualAppearanceToCSAR(repository, (RelationshipTypeId) id);
        } else if (id instanceof NodeTypeId) {
            this.addVisualAppearanceToCSAR(repository, (NodeTypeId) id);
        } else if (id instanceof ArtifactTemplateId) {
            this.prepareForExport(repository, (ArtifactTemplateId) id);
        }
    }

    /**
     * Adds the given id as import to the given imports collection
     */
    protected void addToImports(IRepository repository, DefinitionsChildId id, Collection<TImport> imports) {
        TImport imp = new TImport();
        imp.setImportType(Namespaces.TOSCA_NAMESPACE);
        imp.setNamespace(id.getNamespace().getDecoded());
        URI uri = (URI) this.exportConfiguration.get(CsarExportConfiguration.REPOSITORY_URI.name());
        if (uri == null) {
            // self-contained mode
            // all Definitions are contained in "Definitions" directory, therefore, we provide the filename only
            // references are resolved relatively from a definitions element (COS01, line 425)
            String fn = CsarExporter.getDefinitionsFileName(repository, id);
            fn = EncodingUtil.URLencode(fn);
            imp.setLocation(fn);
        } else {
            String path = Util.getUrlPath(id);
            path = path + "?definitions";
            URI absoluteURI = uri.resolve(path);
            imp.setLocation(absoluteURI.toString());
        }

        imports.add(imp);

        // FIXME: Currently the depended elements (such as the artifact templates linked to a node type implementation) are gathered by the corresponding "addXY" method.
        // Reason: the corresponding TDefinitions element is *not* updated if a related element is added/removed.
        // That means: The imports are not changed.
        // The current issue is that TOSCA allows imports of Definitions only and the repository has the concrete elements as main structure
        // Although during save the import can be updated (by fetching the associated resource and get the definitions of it),
        // The concrete definitions cannot be determined without
        //  a) having a complete index of all definitions in the repository
        //  b) crawling through the *complete* repository
        // Possibly the current solution, just lazily adding all dependent elements is the better solution.
    }

    /**
     * Synchronizes the plan model references and adds the plans to the csar (putRefAsReferencedItemInCsar)
     */
    private void prepareForExport(IRepository repository, ServiceTemplateId id) throws IOException {
        // ensure that the plans stored locally are the same ones as stored in the definitions
        BackendUtils.synchronizeReferences(id, repository);

        // add all plans as reference in the CSAR
        // the data model is consistent with the repository
        // we crawl through the repository to as putRefAsReferencedItemInCsar expects a repository file reference
        PlansId plansContainerId = new PlansId(id);
        SortedSet<PlanId> nestedPlans = repository.getNestedIds(plansContainerId, PlanId.class);
        for (PlanId planId : nestedPlans) {
            SortedSet<RepositoryFileReference> containedFiles = repository.getContainedFiles(planId);
            // even if we currently support only one file in the directory, we just add everything
            for (RepositoryFileReference ref : containedFiles) {
                putRefAsReferencedItemInCsar(repository, ref);
            }
        }
    }

    /**
     * Determines the referenced definition children Ids and also updates the references in the Artifact Template
     *
     * @return a collection of referenced definition child Ids
     */
    protected void prepareForExport(IRepository repository, ArtifactTemplateId id) throws IOException {
        // Export files

        // This method is called BEFORE the concrete definitions element is written.
        // Therefore, we adapt the content of the attached files to the really existing files
        BackendUtils.synchronizeReferences(repository, id);

        DirectoryId fileDir = new ArtifactTemplateFilesDirectoryId(id);
        SortedSet<RepositoryFileReference> files = repository.getContainedFiles(fileDir);
        for (RepositoryFileReference ref : files) {
            // Even if writing a TOSCA only (!this.writingCSAR),
            // we put the virtual path in the TOSCA
            // Reason: Winery is mostly used as a service and local storage
            // reference to not make sense
            // The old implementation had absolutePath.toUri().toString();
            // there, but this does not work when using a cloud blob store.

            putRefAsReferencedItemInCsar(repository, ref);
        }
    }

    /**
     * Puts the given reference as item in the CSAR
     * <p>
     * Thereby, it uses the global variable referencesToPathInCSARMap
     */
    protected void putRefAsReferencedItemInCsar(IRepository repository, RepositoryFileReference ref) {
        // Determine path
        String pathInsideRepo = BackendUtils.getPathInsideRepo(ref);

        // put mapping reference to path into global map
        // the path is the same as put in "synchronizeReferences"
        this.referencesToPathInCSARMap.put(new CsarContentProperties(pathInsideRepo), new RepositoryRefBasedCsarEntry(repository, ref));
    }

    protected void addVisualAppearanceToCSAR(IRepository repository, TopologyGraphElementEntityTypeId id) {
        VisualAppearanceId visId = new VisualAppearanceId(id);
        if (repository.exists(visId)) {
            // we do NOT check for the id, but simply check for bigIcon.png (only exists in NodeType) and smallIcon.png (exists in NodeType and RelationshipType)

            RepositoryFileReference ref = new RepositoryFileReference(visId, Filename.FILENAME_BIG_ICON);
            if (repository.exists(ref)) {
                putRefAsReferencedItemInCsar(repository, ref);
            }

            ref = new RepositoryFileReference(visId, Filename.FILENAME_SMALL_ICON);
            if (repository.exists(ref)) {
                putRefAsReferencedItemInCsar(repository, ref);
            }
        }
    }
}
