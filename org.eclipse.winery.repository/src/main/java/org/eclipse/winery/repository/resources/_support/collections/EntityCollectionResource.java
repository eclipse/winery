/*******************************************************************************
 * Copyright (c) 2012-2014 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Oliver Kopp - initial API and implementation
* 	   Lukas Balzer - added support for angular frontend
 *******************************************************************************/
package org.eclipse.winery.repository.resources._support.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.print.attribute.standard.Media;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.winery.common.Util;
import org.eclipse.winery.repository.Utils;
import org.eclipse.winery.repository.datatypes.select2.Select2DataItem;
import org.eclipse.winery.repository.resources._support.IPersistable;
import org.eclipse.winery.repository.resources._support.collections.withoutid.EntityWithoutIdCollectionResource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class managing a list of entities. It is intended to manage subresources,
 * which are stored in a list. Either all entities have a unique key given by
 * the TOSCA specification (subclass EntityWithIdCollectionResource) or a unique
 * key is generated (subclass EntityWithoutIdCollectionResource)
 *
 * @param <EntityResourceT> the resource modeling the entity
 * @param <EntityT> the entity type of single items in the list
 */
public abstract class EntityCollectionResource<EntityResourceT extends EntityResource<EntityT>, EntityT> implements IIdDetermination<EntityT> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityCollectionResource.class);

	protected final List<EntityT> list;

	protected final IPersistable res;

	protected final Class<EntityT> entityTClazz;

	protected final Class<EntityResourceT> entityResourceTClazz;


	/**
	 * @param entityTClazz the class of EntityT. Required as it is not possible to call
	 *            new EntityT (see http://stackoverflow.com/a/1090488/873282)
	 * @param list the list of entities contained in this resource. Has to be
	 *            typed <Object> as not all TOSCA elements in the specification
	 *            inherit from TExtensibleElements
	 * @param res the main resource the list is belonging to. Required for
	 *            persistence.
	 */
	public EntityCollectionResource(Class<EntityResourceT> entityResourceTClazz, Class<EntityT> entityTClazz, List<EntityT> list, IPersistable res) {
		this.entityResourceTClazz = entityResourceTClazz;
		this.entityTClazz = entityTClazz;
		this.list = list;
		this.res = res;
	}

	public Object getListOfAllEntityIds(@QueryParam("select2") String select2) {
		if (select2 == null) {
			return this.getListOfAllEntityIdsAsList();
		} else {
			// return data ready for consumption by select2
			List<Select2DataItem> res = new ArrayList<>(this.list.size());
			for (EntityT o : this.list) {
				String id = this.getId(o);
				Select2DataItem di = new Select2DataItem(id, id);
				res.add(di);
			}
			return res;
		}
	}

	public List<String> getListOfAllEntityIdsAsList() {
		List<String> res = new ArrayList<>(this.list.size());
		for (EntityT o : this.list) {
			// We assume that different Object serializations *always* have different hashCodes
			res.add(this.getId(o));
		}
		return res;
	}

	/**
	 * XML is currently not possible. One has to use Utils.getXMLAsString((Class<EntityT>) this.o.getClass(), this.o, false);
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getAllEntityResources() {
		List<String> listOfAllSubResources = this.getListOfAllEntityIdsAsList();
		List<EntityResourceT> ressources = new ArrayList<>(listOfAllSubResources.size());
		for (String id : listOfAllSubResources) {
			ressources.add(this.getEntityResourceFromDecodedId(id));
		}
		return ressources.stream().map(res -> {
			String id = this.getId(res.o);
			// some objects already have an id field
			// we set it nevertheless, because it might happen that the name of the id field is not "id", but something else (such as "name")

			this.getEntityResourceFromDecodedId(id);
			// general method, same as with data binding
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			// (note: can also use more specific type, like ArrayNode or
			// ObjectNode!)
			JsonNode jsonNode = mapper.valueToTree(res.o);
			((ObjectNode)jsonNode).put("id", id);
			try {
				return mapper.writeValueAsString(jsonNode);
			} catch (JsonProcessingException e) {
				throw new WebApplicationException(e);
			}
		}).collect(Collectors.joining(",", "[", "]"));
	}

	public EntityResourceT getEntityResourceFromDecodedId(String id) {
		EntityT entity = null;
		int idx = -1;
		for (EntityT c : this.list) {
			idx++;
			String cId = this.getId(c);
			if (cId.equals(id)) {
				entity = c;
				break;
			}
		}
		if (entity == null) {
			throw new NotFoundException();
		} else {
			return this.getEntityResourceInstance(entity, idx);
		}
	}

	@Path("{id}/")
	public EntityResourceT getEntityResource(@PathParam("id") String id) {
		if (id == null) {
			throw new IllegalArgumentException("id has to be given");
		}
		id = Util.URLdecode(id);
		return this.getEntityResourceFromDecodedId(id);
	}

	/**
	 * @param entity the entity to create a resource for
	 * @param idx the index in the list
	 * @return the resource managing the given entity
	 */
	protected abstract EntityResourceT getEntityResourceInstance(EntityT entity, int idx);

	/**
	 * called by getHTMLAsResponse
	 */
	public abstract Viewable getHTML();

	/**
	 * Adds a new entity
	 *
	 * In case the element already exists, we return "CONFLICT"
	 */
	@POST
	public Response addNewElement(EntityT entity) {
		if (entity == null) {
			return Response.status(Status.BAD_REQUEST).entity("a valid XML/JSON element has to be posted").build();
		}
		if (this.alreadyContains(entity)) {
			// we do not replace the element, but replace it
			return Response.status(Status.CONFLICT).build();
		}
		this.list.add(entity);
		return CollectionsHelper.persist(this.res, this, entity, true);
	}

	@Override
	public abstract String getId(EntityT entity);

	/**
	 * Checks for containment of e in the list. <code>equals</code> is not used
	 * as most EntityT do not offer a valid implementation
	 *
	 * @return true if list already contains e.
	 */
	public boolean alreadyContains(EntityT e) {
		String id = this.getId(e);
		for (EntityT el : this.list) {
			if (this.getId(el).equals(id)) {
				// break loop
				// we found an equal list item
				return true;
			}
		}
		// all items checked: nothing equal contained
		return false;
	}

}
