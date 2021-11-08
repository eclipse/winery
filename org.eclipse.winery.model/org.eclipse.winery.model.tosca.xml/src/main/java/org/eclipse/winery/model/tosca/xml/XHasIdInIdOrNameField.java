/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.xml;

import javax.xml.bind.annotation.XmlTransient;

public interface XHasIdInIdOrNameField {

    /**
     * Returns the id
     */
    @XmlTransient
    default String getIdFromIdOrNameField() {
        if (this instanceof XHasId) {
            return ((XHasId) this).getId();
        } else {
            return ((XHasName) this).getName();
        }
    }

    /**
     * Sets the id using the given string. In case the class implements HasId, the id is set using HasId.setId.
     * Otherwise, the name attribute is set
     *
     * @param id the id to set
     */
    default void setId(String id) {
        if (this instanceof XHasId) {
            ((XHasId) this).setId(id);
        }
        if (this instanceof XHasName) {
            ((XHasName) this).setName(id);
        }
    }
}
