/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Karoline Saatkamp - initial API and implementation
 *******************************************************************************/

package org.eclipse.winery.repository.resources._support.dataadapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class InjectionOptions {
	@XmlElement
	private List<InjectionOption> injectionOption = new ArrayList<>();

	public List<InjectionOption> getInjectionOption() {
		return Collections.unmodifiableList(injectionOption);
	}

	public void addInjectionOptions(InjectionOption in) {
		injectionOption.add(in);
	}
}
