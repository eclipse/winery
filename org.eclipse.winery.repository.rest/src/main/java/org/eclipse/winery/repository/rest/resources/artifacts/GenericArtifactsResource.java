/*******************************************************************************
 * Copyright (c) 2012-2023 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.artifacts;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.generators.ia.Generator;
import org.eclipse.winery.model.ids.Namespace;
import org.eclipse.winery.model.ids.XmlId;
import org.eclipse.winery.model.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.model.ids.definitions.ArtifactTypeId;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.ids.definitions.EntityTypeId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TImplementationArtifact;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.constants.QNames;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.FileUtils;
import org.eclipse.winery.repository.common.Util;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateSourceDirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.DirectoryId;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResource;
import org.eclipse.winery.repository.rest.resources._support.INodeTemplateResourceOrNodeTypeImplementationResourceOrRelationshipTypeImplementationResource;
import org.eclipse.winery.repository.rest.resources._support.collections.withid.EntityWithIdCollectionResource;
import org.eclipse.winery.repository.rest.resources.apiData.GenerateArtifactApiData;
import org.eclipse.winery.repository.rest.resources.entitytypeimplementations.nodetypeimplementations.NodeTypeImplementationResource;
import org.eclipse.winery.repository.rest.resources.entitytypeimplementations.nodetypeimplementations.NodeTypeImplementationsResource;
import org.eclipse.winery.repository.rest.resources.entitytypeimplementations.relationshiptypeimplementations.RelationshipTypeImplementationResource;
import org.eclipse.winery.repository.rest.resources.entitytypeimplementations.relationshiptypeimplementations.RelationshipTypeImplementationsResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates.NodeTemplateResource;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * Resource handling both deployment and implementation artifacts
 */
public abstract class GenericArtifactsResource<ArtifactResource extends GenericArtifactResource<ArtifactT>, ArtifactT> extends EntityWithIdCollectionResource<ArtifactResource, ArtifactT> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericArtifactsResource.class);

    protected final INodeTemplateResourceOrNodeTypeImplementationResourceOrRelationshipTypeImplementationResource resWithNamespace;

    public GenericArtifactsResource(Class<ArtifactResource> entityResourceTClazz, Class<ArtifactT> entityTClazz,
                                    List<ArtifactT> list, INodeTemplateResourceOrNodeTypeImplementationResourceOrRelationshipTypeImplementationResource res) {
        super(entityResourceTClazz, entityTClazz, list, GenericArtifactsResource.getAbstractComponentInstanceResource(res));
        this.resWithNamespace = res;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new implementation/deployment artifact. If an implementation artifact with the same name already exists, it is <em>overridden</em>.")
    @SuppressWarnings("unchecked")
    public IAGenerationReport generateArtifact(GenerateArtifactApiData apiData, @Context UriInfo uriInfo, @Context final HttpServletResponse response) throws Exception {
        // we assume that the parent ComponentInstance container exists

        final IRepository repository = RepositoryFactory.getRepository();
        if (StringUtils.isEmpty(apiData.artifactName)) {
            throw new IllegalArgumentException("Empty artifactName");
        }
        if (StringUtils.isEmpty(apiData.artifactType)) {
            if (StringUtils.isEmpty(apiData.artifactTemplateName) || StringUtils.isEmpty(apiData.artifactTemplateNamespace)) {
                if (StringUtils.isEmpty(apiData.artifactTemplate)) {
                    throw new IllegalArgumentException("No artifact type given and no template given. Cannot guess artifact type");
                }
            }
        }

        if (!StringUtils.isEmpty(apiData.autoGenerateIA)) {
            if (StringUtils.isEmpty(apiData.interfaceName)) {
                throw new IllegalArgumentException("No interface name supplied for IA auto generation.");
            }
        }

        // convert second calling form to first calling form
        if (!StringUtils.isEmpty(apiData.artifactTemplate)) {
            QName qname = QName.valueOf(apiData.artifactTemplate);
            apiData.artifactTemplateName = qname.getLocalPart();
            apiData.artifactTemplateNamespace = qname.getNamespaceURI();
        }

        Document doc = null;

        // check artifact specific content for validity
        // if invalid, abort and do not create anything
        if (!StringUtils.isEmpty(apiData.artifactSpecificContent)) {
            try {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputSource is = new InputSource();
                StringReader sr = new StringReader(apiData.artifactSpecificContent);
                is.setCharacterStream(sr);
                doc = db.parse(is);
            } catch (Exception e) {
                // FIXME: currently we allow a single element only. However, the content should be internally wrapped by an (arbitrary) XML element as the content will be nested in the artifact element, too
                LOGGER.debug("Invalid content", e);
                throw e;
            }
        }

        // determine artifactTemplate and artifactType

        ArtifactTypeId artifactTypeId;
        ArtifactTemplateId artifactTemplateId = null;

        boolean doAutoCreateArtifactTemplate = !(StringUtils.isEmpty(apiData.autoCreateArtifactTemplate) || apiData.autoCreateArtifactTemplate.equalsIgnoreCase("no") || apiData.autoCreateArtifactTemplate.equalsIgnoreCase("false"));
        if (!doAutoCreateArtifactTemplate) {
            // no auto creation
            if (!StringUtils.isEmpty(apiData.artifactTemplateName) && !StringUtils.isEmpty(apiData.artifactTemplateNamespace)) {
                QName artifactTemplateQName = new QName(apiData.artifactTemplateNamespace, apiData.artifactTemplateName);
                artifactTemplateId = BackendUtils.getDefinitionsChildId(ArtifactTemplateId.class, artifactTemplateQName);
            }
            if (StringUtils.isEmpty(apiData.artifactType)) {
                // derive the type from the artifact template
                if (artifactTemplateId == null) {
                    throw new IllegalArgumentException("No artifactTemplate and no artifactType provided. Deriving the artifactType is not possible.");
                }
                @NonNull final QName type = repository.getElement(artifactTemplateId).getType();
                artifactTypeId = BackendUtils.getDefinitionsChildId(ArtifactTypeId.class, type);
            } else {
                // artifactTypeStr is directly given, use that
                artifactTypeId = BackendUtils.getDefinitionsChildId(ArtifactTypeId.class, apiData.artifactType);
            }
        } else {
            // do the artifact template auto creation magic

            if (StringUtils.isEmpty(apiData.artifactType)) {
                throw new IllegalArgumentException("Artifact template auto creation requested, but no artifact type supplied.");
            }

            artifactTypeId = BackendUtils.getDefinitionsChildId(ArtifactTypeId.class, apiData.artifactType);
            // ensure that given type exists
            if (!repository.exists(artifactTypeId)) {
                LOGGER.debug("Artifact type {} is created", apiData.artifactType);
                final TArtifactType element = repository.getElement(artifactTypeId);
                try {
                    repository.setElement(artifactTypeId, element);
                } catch (IOException e) {
                    throw new WebApplicationException(e);
                }
            }

            if (StringUtils.isEmpty(apiData.artifactTemplateName) || StringUtils.isEmpty(apiData.artifactTemplateNamespace)) {
                // no explicit name provided
                // we use the artifactNameStr as prefix for the
                // artifact template name

                // we create a new artifact template in the namespace of the parent
                // element
                Namespace namespace = this.resWithNamespace.getNamespace();

                artifactTemplateId = new ArtifactTemplateId(namespace, new XmlId(apiData.artifactName + "artifactTemplate", false));
            } else {
                QName artifactTemplateQName = new QName(apiData.artifactTemplateNamespace, apiData.artifactTemplateName);
                artifactTemplateId = new ArtifactTemplateId(artifactTemplateQName);
            }

            // even if artifactTemplate does not exist, it is loaded
            final TArtifactTemplate artifactTemplate = repository.getElement(artifactTemplateId);
            artifactTemplate.setType(artifactTypeId.getQName());
            try {
                repository.setElement(artifactTemplateId, artifactTemplate);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        }

        // variable artifactTypeId is set
        // variable artifactTemplateId is not null if artifactTemplate has been generated

        // we have to generate the DA/IA resource now
        // Doing it here instead of doing it at the subclasses is dirty on the
        // one hand, but quicker to implement on the other hand

        // Create the artifact itself

        ArtifactT resultingArtifact;

        if (this instanceof ImplementationArtifactsResource) {
            TImplementationArtifact.Builder builder = new TImplementationArtifact.Builder(artifactTypeId.getQName())
                .setName(apiData.artifactName)
                .setInterfaceName(apiData.interfaceName)
                .setOperationName(apiData.operationName);
            if (artifactTemplateId != null) {
                builder.setArtifactRef(artifactTemplateId.getQName());
            }
            if (doc != null) {
                // the content has been checked for validity at the beginning of the method.
                // If this point in the code is reached, the XML has been parsed into doc
                // just copy over the dom node. Hopefully, that works...
                builder.setAny(Collections.singletonList(doc.getDocumentElement()));
            }

            resultingArtifact = (ArtifactT) builder.build();
        } else {
            // for comments see other branch

            TDeploymentArtifact.Builder builder = new TDeploymentArtifact.Builder(apiData.artifactName, artifactTypeId.getQName());
            if (artifactTemplateId != null) {
                builder.setArtifactRef(artifactTemplateId.getQName());
            }
            if (doc != null) {
                builder.setAny(Collections.singletonList(doc.getDocumentElement()));
            }

            resultingArtifact = (ArtifactT) builder.build();
        }
        this.list.add(resultingArtifact);

        // TODO: Check for error, and in case one found return it
        RestUtils.persist(super.res);

        response.setStatus(Status.CREATED.getStatusCode());
        try {
            response.flushBuffer();
        } catch (Exception e) {
        }
        if (StringUtils.isEmpty(apiData.autoGenerateIA)) {
            // No IA generation

            if (artifactTemplateId != null) {
                return new IAGenerationReport(URI.create(RestUtils.getAbsoluteURL(artifactTemplateId)).toURL());
            }

            return new IAGenerationReport();
        } else {
            LOGGER.debug("\nArtifact API Data: ArtifactName:{},\nArtifactTemplate: {},\n ArtifactTemplateName: {}\n ArtifactType: {},\n InterfaceName: {},\nOperationName: {}", apiData.artifactName, apiData.artifactTemplate, apiData.artifactTemplateName, apiData.artifactType, apiData.interfaceName, apiData.operationName);
            // after everything was created, we fire up the artifact generation
            return this.generateImplementationArtifact(apiData.interfaceName, apiData.javaPackage, uriInfo, artifactTemplateId, apiData.artifactType, apiData.operationName);
        }
    }

    /**
     * Generates a unique and valid name to be used for the generated maven project name, java project name, class name,
     * port type name.
     */
    private String generateName(EntityTypeId typeId, String interfaceName) {
        String name = Util.namespaceToJavaPackage(typeId.getNamespace().getDecoded());
        name += Util.FORBIDDEN_CHARACTER_REPLACEMENT;

        // Winery already ensures that this is a valid NCName
        // getName() returns the id of the nodeType: A nodeType carries the "id" attribute only (and no name attribute)
        name += VersionUtils.getNameWithoutVersion(typeId.getXmlId().getDecoded());

        // Two separators to distinguish node type and interface part
        name += Util.FORBIDDEN_CHARACTER_REPLACEMENT;
        name += Util.FORBIDDEN_CHARACTER_REPLACEMENT;
        name += Util.namespaceToJavaPackage(interfaceName);

        // In addition we must replace '.', because Java class names must not
        // contain dots, but for Winery they are fine.
        return name.replace(".", Util.FORBIDDEN_CHARACTER_REPLACEMENT);
    }

    /**
     * Generates the implementation artifact using the implementation artifact generator. Also sets the properties
     * according to the requirements of OpenTOSCA.
     */
    private IAGenerationReport generateImplementationArtifact(String interfaceName, String javaPackage, UriInfo uriInfo, ArtifactTemplateId artifactTemplateId, String artifactType, String operation) throws Exception {
        assert (this instanceof ImplementationArtifactsResource);
        IRepository repository = RepositoryFactory.getRepository();

        QName type = RestUtils.getType(this.res);
        EntityTypeId typeId = getTypeId(type).orElseThrow(IllegalStateException::new);
        TInterface tInterface = findInterface(typeId, interfaceName).orElseThrow(IllegalStateException::new);

        Path workingDir;
        try {
            workingDir = Files.createTempDirectory("winery");
        } catch (IOException e2) {
            LOGGER.debug("Could not create temporary directory", e2);
            throw new IOException("Could not create temporary directory");
        }

        URI artifactTemplateFilesUri = uriInfo.getBaseUri().resolve(RestUtils.getAbsoluteURL(artifactTemplateId)).resolve("files");
        URL artifactTemplateFilesUrl;
        try {
            artifactTemplateFilesUrl = artifactTemplateFilesUri.toURL();
        } catch (MalformedURLException e2) {
            LOGGER.debug("Could not convert URI to URL", e2);
            throw new MalformedURLException("Could not convert URI to URL");
        }

        IAGenerationReport result = new IAGenerationReport();
        String name = this.generateName(typeId, interfaceName);
        Generator gen = Generator.getGenerator(artifactType, tInterface, javaPackage, artifactTemplateFilesUrl, name, workingDir, operation);
        Path targetPath;
        try {
            targetPath = gen.generateArtifact();
            DirectoryId fileDir = new ArtifactTemplateSourceDirectoryId(artifactTemplateId);
            BackendUtils.importDirectory(targetPath, repository, fileDir);
        } catch (IllegalArgumentException iaEx) {
            LOGGER.debug("IA stub generation failed", iaEx);
            result.warning = "IA stub generation failed, as selected artifact type does not support interface level generation.";
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }

        // clean up
        FileUtils.forceDelete(workingDir);

        this.storeProperties(artifactTemplateId, typeId, name);

        result.artifactTemplate = uriInfo.getBaseUri().resolve(Util.getUrlPath(artifactTemplateId)).toURL();
        return result;
    }

    private static class IAGenerationReport {
        public String warning = "";
        public URL artifactTemplate;

        public IAGenerationReport() {
        }

        public IAGenerationReport(URL artifactTemplate) {
            this.artifactTemplate = artifactTemplate;
        }
    }

    private Optional<EntityTypeId> getTypeId(QName type) {
        if (this.res instanceof NodeTypeImplementationResource
            || this.res instanceof NodeTypeImplementationsResource) {
            return Optional.of(new NodeTypeId(type));
        } else if (this.res instanceof RelationshipTypeImplementationResource
            || this.res instanceof RelationshipTypeImplementationsResource) {
            return Optional.of(new RelationshipTypeId(type));
        }
        return Optional.empty();
    }

    private Optional<TInterface> findInterface(EntityTypeId id, String interfaceName) {
        TInterface i;
        List<TInterface> interfaces = new ArrayList<>();
        IRepository repository = RepositoryFactory.getRepository();
        if (this.res instanceof NodeTypeImplementationResource
            || this.res instanceof NodeTypeImplementationsResource) {
            TNodeType nodeType = repository.getElement((NodeTypeId) id);
            if (nodeType.getInterfaces() != null) {
                interfaces.addAll(nodeType.getInterfaces());
            }
        } else if (this.res instanceof RelationshipTypeImplementationResource
            || this.res instanceof RelationshipTypeImplementationsResource) {
            TRelationshipType relationshipType = repository.getElement((RelationshipTypeId) id);
            if (relationshipType.getSourceInterfaces() != null) {
                interfaces.addAll(relationshipType.getSourceInterfaces());
            }
            if (relationshipType.getTargetInterfaces() != null) {
                interfaces.addAll(relationshipType.getTargetInterfaces());
            }
            if (relationshipType.getInterfaces() != null) {
                interfaces.addAll(relationshipType.getInterfaces());
            }
        }
        Iterator<TInterface> it = interfaces.iterator();
        do {
            i = it.next();
            if (i.getName().equals(interfaceName)) {
                return Optional.of(i);
            }
        } while (it.hasNext());
        return Optional.empty();
    }

    private void storeProperties(ArtifactTemplateId artifactTemplateId, DefinitionsChildId typeId, String name) {
        // We generate the properties by hand instead of using JAX-B as using JAX-B causes issues at org.eclipse.winery.common.ModelUtilities.getPropertiesKV(TEntityTemplate):
        // getAny() does not always return "w3c.dom.element" anymore

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String FEATURE = null;
        try {
            FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            dbf.setFeature(FEATURE, false);

            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            dbf.setFeature(FEATURE, false);

            FEATURE = "http://xml.org/sax/features/external-general-entities";
            dbf.setFeature(FEATURE, false);

            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("The feature '"
                + FEATURE + "' is not supported by your XML processor.", e);
        }
        DocumentBuilder builder;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }
        Document doc = builder.newDocument();
        String namespace = QNames.QNAME_ARTIFACT_TYPE_WAR.getNamespaceURI();
        Element root = doc.createElementNS(namespace, "WSProperties");
        doc.appendChild(root);

        Element element = doc.createElementNS(namespace, "ServiceEndpoint");
        Text text = doc.createTextNode("/services/" + name + "Port");
        element.appendChild(text);
        root.appendChild(element);

        element = doc.createElementNS(namespace, "PortType");
        text = doc.createTextNode("{" + typeId.getNamespace().getDecoded() + "}" + name);
        element.appendChild(text);
        root.appendChild(element);

        element = doc.createElementNS(namespace, "InvocationType");
        text = doc.createTextNode("SOAP/HTTP");
        element.appendChild(text);
        root.appendChild(element);

        TEntityTemplate.XmlProperties properties = new TEntityTemplate.XmlProperties();
        properties.setAny(root);

        final IRepository repository = RepositoryFactory.getRepository();
        final TArtifactTemplate artifactTemplate = repository.getElement(artifactTemplateId);
        artifactTemplate.setProperties(properties);
        try {
            repository.setElement(artifactTemplateId, artifactTemplate);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * Required for artifacts.jsp
     *
     * @return list of all contained artifacts.
     */
    public abstract Collection<ArtifactResource> getAllArtifactResources();

    /**
     * required by artifacts.jsp
     */
    public String getNamespace() {
        return this.resWithNamespace.getNamespace().getDecoded();
    }

    /**
     * For saving resources, an AbstractComponentInstanceResource is required. DAs may be attached to a node template,
     * which is not an AbstractComponentInstanceResource, but its grandparent resource ServiceTemplate is
     *
     * @param res the resource to determine the the AbstractComponentInstanceResource for
     * @return the AbstractComponentInstanceResource where the given res is contained in
     */
    public static AbstractComponentInstanceResource getAbstractComponentInstanceResource(INodeTemplateResourceOrNodeTypeImplementationResourceOrRelationshipTypeImplementationResource res) {
        final AbstractComponentInstanceResource r;
        if (res instanceof NodeTemplateResource) {
            r = ((NodeTemplateResource) res).getServiceTemplateResource();
        } else {
            // quick hack: the resource has to be an abstract component instance
            r = (AbstractComponentInstanceResource) res;
        }
        return r;
    }
}
