/*******************************************************************************
 * Copyright (c) 2012-2013 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Lukas Harzenetter - initial API and implementation
 *******************************************************************************/
package org.eclipse.winery.repository.resources.apiData;

public class InstanceStateApiData {

    public String state;

    public InstanceStateApiData() {
	}

	public InstanceStateApiData(String state) {
        this.state = state;
    }

    public String toString() {
        return "state: " + this.state;
    }

}
