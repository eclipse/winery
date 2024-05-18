/********************************************************************************
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
 ********************************************************************************/
package org.eclipse.winery.repository.rest.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.configuration.Environments;
import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.edmm.EdmmUtils;
import org.eclipse.winery.edmm.model.EdmmImporter;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.GitBasedRepository;
import org.eclipse.winery.repository.importing.CsarImportOptions;
import org.eclipse.winery.repository.importing.CsarImporter;
import org.eclipse.winery.repository.importing.ImportMetaInformation;
import org.eclipse.winery.repository.importing.YamlCsarImporter;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.datatypes.ComponentId;
import org.eclipse.winery.repository.rest.resources.API.APIResource;
import org.eclipse.winery.repository.rest.resources.admin.AdminTopResource;
import org.eclipse.winery.repository.rest.resources.compliancerules.ComplianceRulesResource;
import org.eclipse.winery.repository.rest.resources.dataflowmodels.DataFlowResource;
import org.eclipse.winery.repository.rest.resources.entitytemplates.artifacttemplates.ArtifactTemplatesResource;
import org.eclipse.winery.repository.rest.resources.entitytemplates.policytemplates.PolicyTemplatesResource;
import org.eclipse.winery.repository.rest.resources.entitytypeimplementations.nodetypeimplementations.NodeTypeImplementationsResource;
import org.eclipse.winery.repository.rest.resources.entitytypeimplementations.relationshiptypeimplementations.RelationshipTypeImplementationsResource;
import org.eclipse.winery.repository.rest.resources.entitytypes.artifacttypes.ArtifactTypesResource;
import org.eclipse.winery.repository.rest.resources.entitytypes.capabilitytypes.CapabilityTypesResource;
import org.eclipse.winery.repository.rest.resources.entitytypes.nodetypes.NodeTypesResource;
import org.eclipse.winery.repository.rest.resources.entitytypes.policytypes.PolicyTypesResource;
import org.eclipse.winery.repository.rest.resources.entitytypes.relationshiptypes.RelationshipTypesResource;
import org.eclipse.winery.repository.rest.resources.entitytypes.requirementtypes.RequirementTypesResource;
import org.eclipse.winery.repository.rest.resources.imports.ImportsResource;
import org.eclipse.winery.repository.rest.resources.refinementmodels.PatternRefinementModelsResource;
import org.eclipse.winery.repository.rest.resources.refinementmodels.TestRefinementModelsResource;
import org.eclipse.winery.repository.rest.resources.refinementmodels.TopologyFragmentRefinementModelsResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.ServiceTemplatesResource;
import org.eclipse.winery.repository.rest.resources.threats.ThreatsResource;
import org.eclipse.winery.repository.rest.resources.yaml.DataTypesResource;
import org.eclipse.winery.repository.rest.resources.yaml.YAMLParserResource;
import org.eclipse.winery.repository.yaml.YamlRepository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.common.configuration.RepositoryConfigurationObject.RepositoryProvider.YAML;

/**
 * All paths listed here have to be listed in Jersey's filter configuration
 */
@Api()
@Path("/")
public class MainResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainResource.class);

    @Path("API/")
    public APIResource api() {
        return new APIResource();
    }

    @Path("artifacttemplates/")
    public ArtifactTemplatesResource artifacttemplates() {
        return new ArtifactTemplatesResource();
    }

    @Path("artifacttypes/")
    public ArtifactTypesResource artifactypes() {
        return new ArtifactTypesResource();
    }

    @Path("admin/")
    public AdminTopResource admin() {
        return new AdminTopResource();
    }

    @Path("capabilitytypes/")
    public CapabilityTypesResource capabilitytypes() {
        return new CapabilityTypesResource();
    }

    @Path("datatypes/")
    public DataTypesResource datatypes() {
        return new DataTypesResource();
    }

    @Path("imports/")
    public ImportsResource imports() {
        return new ImportsResource();
    }

    @Path("nodetypes/")
    public NodeTypesResource nodetypes() {
        return new NodeTypesResource();
    }

    @Path("nodetypeimplementations/")
    public NodeTypeImplementationsResource nodetypeimplementations() {
        return new NodeTypeImplementationsResource();
    }

    @Path("policytemplates/")
    public PolicyTemplatesResource policytemplates() {
        return new PolicyTemplatesResource();
    }

    @Path("policytypes/")
    public PolicyTypesResource policytypes() {
        return new PolicyTypesResource();
    }

    @Path("relationshiptypes/")
    public RelationshipTypesResource relationshiptypes() {
        return new RelationshipTypesResource();
    }

    @Path("requirementtypes/")
    public RequirementTypesResource requirementtypes() {
        return new RequirementTypesResource();
    }

    @Path("relationshiptypeimplementations/")
    public RelationshipTypeImplementationsResource relationshiptypeimplementations() {
        return new RelationshipTypeImplementationsResource();
    }

    @Path("servicetemplates/")
    public ServiceTemplatesResource servicetemplates() {
        return new ServiceTemplatesResource();
    }

    @Path("compliancerules/")
    public ComplianceRulesResource compliancerules() {
        return new ComplianceRulesResource();
    }

    @Path("patternrefinementmodels/")
    public PatternRefinementModelsResource patternRefinementModels() {
        return new PatternRefinementModelsResource();
    }

    @Path("topologyfragmentrefinementmodels/")
    public TopologyFragmentRefinementModelsResource componentRefinementModels() {
        return new TopologyFragmentRefinementModelsResource();
    }

    @Path("testrefinementmodels/")
    public TestRefinementModelsResource testRefinementModelsResource() {
        return new TestRefinementModelsResource();
    }

    @Path("dataflowmodels/")
    public DataFlowResource dataFlowModels() {
        return new DataFlowResource();
    }

    @Path("yaml/")
    public YAMLParserResource yamlParser() {
        return new YAMLParserResource();
    }

    @Path("threats")
    public ThreatsResource threats() {
        return new ThreatsResource();
    }

    /**
     * Returns the main page of winery.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response onGet() {
        return Response.ok("<html>\n"
            + "<body>\n"
            + "<p>\n"
            + "This is Winery's API.\n"
            + "Please open the Angular UI.\n"
            + "In <strong>development mode</strong>, it is available on <a href=\"http://localhost:4200/\">http://localhost:4200/</a> (ng serve) or <a href=\"http://localhost:8080/\">http://localhost:8080/</a> (configured in IntelliJ).\n"
            + "</p>\n"
            + "\n"
            + "<p>\n"
            + "Configuration of IntelliJ is described <a href=\"http://eclipse.github.io/winery/dev/config/IntelliJ%20IDEA/\">here</a>.\n"
            + "</p>\n"
            + "\n"
            + "<p>Swagger API description is available at <a href=\"http://localhost:8080/winery/swagger\">http://localhost:8080/winery/swagger</a>.\n"
            + "Open <a href=\"http://petstore.swagger.io/\">the Swagger Petsore</a>.\n"
            + "Key in <code>http://localhost:8080/winery/swagger</code> as URL at the top of the page.\n"
            + "</p>\n"
            + "</body>\n"
            + "\n").build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @ApiOperation(value = "Imports the given CSAR (sent by simplesinglefileupload.jsp)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "success",
            responseHeaders = @ResponseHeader(description = "If the CSAR could be partially imported, the points where it failed are returned in the body"))
    })
    // @formatter:off
    public Response importCSAR(
        @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail,
        @ApiParam(value = "true: content of CSAR overwrites existing content. false (default): existing content is kept") @FormDataParam("overwrite") Boolean overwrite,
        @ApiParam(value = "true: validates the hash of the manifest file with the one stored in the accountability layer") @FormDataParam("validate") Boolean validate,
        @ApiParam(value = "true: interprets the file as an EDMM deployment model and transforms it to TOSCA") @FormDataParam("edmm") Boolean edmm,
        @Context UriInfo uriInfo) {
        LocalDateTime start = LocalDateTime.now();
        // @formatter:on

        if (edmm != null && edmm) {
            new EdmmImporter().importFromStream(uploadedInputStream, fileDetail.getFileName(), overwrite);
            return Response.noContent().build();
        }
        
        CsarImporter importer;
        IRepository repository = RepositoryFactory.getRepository();
        if (Environments.getInstance().getUiConfig().getFeatures().get(YAML.toString())) {
            if (repository instanceof GitBasedRepository) {
                importer = new YamlCsarImporter((YamlRepository) ((GitBasedRepository) repository).getRepository());
            } else {
                importer = new YamlCsarImporter((YamlRepository) repository);
            }
        } else {
            importer = new CsarImporter(repository);
        }

        CsarImportOptions options = new CsarImportOptions();
        options.setOverwrite((overwrite != null) && overwrite);
        options.setAsyncWPDParsing(false);
        options.setValidate((validate != null) && validate);
        ImportMetaInformation importMetaInformation;
        try {
            importMetaInformation = importer.readCSAR(uploadedInputStream, options);
        } catch (Exception e) {
            return Response.serverError().entity("Could not import CSAR").entity(e.getMessage()).build();
        }
        if (importMetaInformation.errors.isEmpty()) {
            if (options.isValidate()) {
                return Response.ok(importMetaInformation, MediaType.APPLICATION_JSON).build();
            } else if (Objects.nonNull(importMetaInformation.entryServiceTemplate)) {
                URI url = uriInfo.getBaseUri().resolve(RestUtils.getAbsoluteURL(importMetaInformation.entryServiceTemplate));
                LOGGER.debug("CSAR import lasted {}", Duration.between(LocalDateTime.now(), start).toString());
                return Response.created(url).build();
            } else {
                LOGGER.debug("CSAR import lasted {}", Duration.between(LocalDateTime.now(), start).toString());
                return Response.noContent().build();
            }
        } else {
            LOGGER.debug("CSAR import lasted {}", Duration.between(LocalDateTime.now(), start).toString());
            // In case there are errors, we send them as "bad request"
            return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(importMetaInformation).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importDefinitions(InputStream is) throws IOException {
        File toscaFile;
        toscaFile = Files.createTempFile("TOSCA", ".tosca").toFile();
        FileUtils.copyInputStreamToFile(is, toscaFile);
        CsarImporter importer = new CsarImporter(RepositoryFactory.getRepository());
        List<String> errors = new ArrayList<>();
        CsarImportOptions options = new CsarImportOptions();
        options.setOverwrite(false);
        options.setAsyncWPDParsing(true);
        options.setValidate(false);
        importer.importDefinitions(null, toscaFile.toPath(), errors, options);

        if (errors.isEmpty()) {
            return Response.noContent().build();
        } else {
            return Response.status(Status.BAD_REQUEST).entity(errors).build();
        }
    }

    @GET
    @Path("toscaLightModels")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ComponentId> getToscaLightModels() {
        return EdmmUtils.getAllToscaLightCompliantModels().entrySet()
            .stream()
            .map(entry -> {
                QName qName = entry.getKey();
                TServiceTemplate serviceTemplate = entry.getValue();
                return new ComponentId(qName.getLocalPart(), serviceTemplate.getName(), qName.getNamespaceURI(),
                    qName, null, VersionUtils.getVersion(qName.getLocalPart())
                );
            })
            .collect(Collectors.toList());
    }
}
