/*******************************************************************************
 * Copyright (c) 2012-2013 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v20.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Oliver Kopp - initial API and implementation
 *******************************************************************************/
package org.eclipse.winery.repository.rest.resources._support.collections.withoutid;

import java.util.List;

import org.eclipse.winery.repository.rest.resources._support.IPersistable;
import org.eclipse.winery.repository.rest.resources._support.collections.EntityResource;
import org.eclipse.winery.repository.rest.resources._support.collections.IIdDetermination;

/**
 * {@inheritDoc}
 */
public abstract class EntityWithoutIdResource<EntityT> extends EntityResource<EntityT> {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public EntityWithoutIdResource(EntityT o, int idx, List<EntityT> list, IPersistable res) {
		super((IIdDetermination<EntityT>) IdDeterminationWithHashCode.INSTANCE, o, idx, list, res);
	}

}
