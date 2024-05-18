/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.edmm;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.winery.edmm.plugins.PluginManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class EdmmTests {

    protected static final Logger LOGGER = LoggerFactory.getLogger(EdmmTests.class);

    @Test
    public void testPluginListEmpty() {
        // testing that the list is not empty
        PluginManager pluginManager = PluginManager.getInstance();
        Assertions.assertFalse(pluginManager.getPluginsList().isEmpty());
    }

    @Test
    public void testPluginListSize() {
        // testing that the list size matches with the decleared plugins in the xml file
        PluginManager pluginManager = PluginManager.getInstance();

        Document xmlPluginConfigDocument;
        // checking if the 
        try {
            InputStream pluginsInputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("pluginContext.xml");

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            String FEATURE = null;
            try {
                FEATURE = "http://xml.org/sax/features/external-parameter-entities";
                documentBuilderFactory.setFeature(FEATURE, false);

                FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
                documentBuilderFactory.setFeature(FEATURE, false);

                FEATURE = "http://xml.org/sax/features/external-general-entities";
                documentBuilderFactory.setFeature(FEATURE, false);

                documentBuilderFactory.setXIncludeAware(false);
                documentBuilderFactory.setExpandEntityReferences(false);

                documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("The feature '"
                    + FEATURE + "' is not supported by your XML processor.", e);
            }
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            xmlPluginConfigDocument = documentBuilder.parse(pluginsInputStream);

            NodeList pluginsNodeList = xmlPluginConfigDocument.getElementsByTagName("bean");
            int pluginsNodeListLen = pluginsNodeList.getLength();

            Assertions.assertEquals(pluginsNodeListLen, pluginManager.getPluginsList().size());
        } catch (Exception e) {
            Assertions.fail();
        }
    }
}
