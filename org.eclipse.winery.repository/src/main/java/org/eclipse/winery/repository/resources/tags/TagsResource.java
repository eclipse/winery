/**
 * Copyright (c) 2012-2013, 2016 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Oliver Kopp - initial API and implementation
 *     Kálmán Képes - refined tag suport
 *     Lukas Balzer - added support for angular frontend
 *******************************************************************************/
package org.eclipse.winery.repository.resources.tags;

import java.util.List;

import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.repository.resources._support.IPersistable;
import org.eclipse.winery.repository.resources._support.collections.withoutid.EntityWithoutIdCollectionResource;

public class TagsResource extends EntityWithoutIdCollectionResource<TagResource, TTag> {

	public TagsResource(IPersistable res, List<TTag> list) {
		super(TagResource.class, TTag.class, list, res);
	}
}
