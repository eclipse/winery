/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.export.entries;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.repository.backend.IRepository;

public class DefinitionsBasedCsarEntry implements CsarEntry {

    private final TDefinitions definitions;
    private final IRepository repository;

    public DefinitionsBasedCsarEntry(TDefinitions definitions, IRepository repository) {
        this.repository = Objects.requireNonNull(repository);
        this.definitions = Objects.requireNonNull(definitions);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        writeToOutputStream(serialized);
        return new ByteArrayInputStream(serialized.toByteArray());
    }

    @Override
    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        repository.serialize(definitions, outputStream);
    }
}
