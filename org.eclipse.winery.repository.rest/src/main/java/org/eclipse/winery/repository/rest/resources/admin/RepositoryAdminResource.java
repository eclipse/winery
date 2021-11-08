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
package org.eclipse.winery.repository.rest.resources.admin;

import java.io.InputStream;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.winery.model.ids.definitions.ArtifactTypeId;
import org.eclipse.winery.model.ids.definitions.CapabilityTypeId;
import org.eclipse.winery.model.ids.definitions.DataTypeId;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.PolicyTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.RequirementTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.GitBasedRepository;
import org.eclipse.winery.repository.filebased.MultiRepository;
import org.eclipse.winery.repository.filebased.MultiRepositoryManager;
import org.eclipse.winery.repository.backend.filebased.RepositoryProperties;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryAdminResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryAdminResource.class);

    /**
     * Imports the given ZIP
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importRepositoryDump(@FormDataParam("file") InputStream uploadedInputStream,
                                         @FormDataParam("file") FormDataContentDisposition fileDetail) {
        RepositoryFactory.getRepository().doImport(uploadedInputStream);
        return Response.noContent().build();
    }

    @DELETE
    public void deleteRepositoryData() {
        RepositoryFactory.getRepository().doClear();
    }

    @GET
    @Produces(org.eclipse.winery.common.constants.MimeTypes.MIMETYPE_ZIP)
    public Response dumpRepository() {
        StreamingOutput so = output -> RepositoryFactory.getRepository().doDump(output);
        return Response.ok()
            .header("Content-Disposition", "attachment;filename=\"repository.zip\"")
            .type(org.eclipse.winery.common.constants.MimeTypes.MIMETYPE_ZIP)
            .entity(so)
            .build();
    }

    /**
     * Deletes given repository
     *
     * @param name of repository to delete.
     */
    @DELETE
    @Path("repositories/{repository}")
    public Response deleteRepository(@PathParam("repository") String name) {
        String repositoryUrl = "";

        if (RepositoryFactory.getRepository() instanceof MultiRepository) {
            IRepository gitRepo;
            MultiRepositoryManager multiRepositoryManager = new MultiRepositoryManager();

            for (RepositoryProperties repo : multiRepositoryManager.getRepositoriesAsList()) {
                if (repo.getName().equals(name)) {
                    repositoryUrl = repo.getUrl();
                    break;
                }
            }

            for (IRepository repo : ((MultiRepository) RepositoryFactory.getRepository()).getRepositories()) {
                gitRepo = repo;

                if (gitRepo instanceof GitBasedRepository) {
                    if (((GitBasedRepository) gitRepo).getRepositoryUrl() != null) {
                        if (((GitBasedRepository) gitRepo).getRepositoryUrl().equals(repositoryUrl)) {
                            ((GitBasedRepository) gitRepo).forceClear();
                            ((MultiRepository) RepositoryFactory.getRepository()).removeRepository(repositoryUrl);
                            break;
                        }
                    }
                }
            }
        }
        return Response.ok().build();
    }

    /**
     * returns List of Repositories to frontend
     *
     * @return List of Repositories
     */
    @GET
    @Path("repositories")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RepositoryProperties> getRepositoriesAsJson() {
        MultiRepositoryManager multiRepositoryManager = new MultiRepositoryManager();
        return multiRepositoryManager.getRepositoriesAsList();
    }

    /**
     * get Repositories from frontend
     */
    @POST
    @Path("repositories")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRepository(List<RepositoryProperties> repositoriesList) {
        if (repositoriesList == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Repositories list must be given.")
                .build();
        }

        MultiRepositoryManager multiRepositoryManager = new MultiRepositoryManager();
        multiRepositoryManager.addRepositoryToFile(repositoriesList);
        return Response.ok().build();
    }

    @POST
    @Path("touch")
    public Response touchAllDefinitions() {
        try {
            IRepository repository = RepositoryFactory.getRepository();
            SortedSet<DefinitionsChildId> definitionIds = Stream.of(
                ArtifactTypeId.class,
                CapabilityTypeId.class,
                NodeTypeId.class,
                PolicyTypeId.class,
                RelationshipTypeId.class,
                RequirementTypeId.class,
                ServiceTemplateId.class,
                DataTypeId.class
            ).flatMap(id -> repository.getAllDefinitionsChildIds(id).stream())
                .collect(Collectors.toCollection(TreeSet::new));
            for (DefinitionsChildId id : definitionIds) {
                TDefinitions definitions = repository.getDefinitions(id);
                BackendUtils.persist(repository, id, definitions);
            }
            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error("Error touching types: {}", e.getMessage(), e);
            return Response.serverError().build();
        }
    }
}
