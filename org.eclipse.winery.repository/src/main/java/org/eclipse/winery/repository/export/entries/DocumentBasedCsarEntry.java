/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Provides access to an entry that represents a (generated) xml Document
 */
public class DocumentBasedCsarEntry implements CsarEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentBasedCsarEntry.class);
    private static Transformer transformer;
    private Document document;

    public DocumentBasedCsarEntry(Document document) {
        this.document = document;
    }

    private static Transformer getTransformer() {
        if (transformer == null) {
            // used for generated XSD schemas
            TransformerFactory tFactory = TransformerFactory.newInstance();
            tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                transformer = tFactory.newTransformer();
            } catch (TransformerConfigurationException e1) {
                LOGGER.debug(e1.getMessage(), e1);
                throw new IllegalStateException("Could not instantiate transformer", e1);
            }
        }

        return transformer;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(byteArrayOutputStream);

        try {
            getTransformer().transform(source, result);
            byte[] bytes = byteArrayOutputStream.toByteArray();

            return new ByteArrayInputStream(bytes);
        } catch (TransformerException | IllegalStateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(outputStream);

        try {
            getTransformer().transform(source, result);
        } catch (TransformerException | IllegalStateException e) {
            throw new IOException(e);
        }
    }
    
}
