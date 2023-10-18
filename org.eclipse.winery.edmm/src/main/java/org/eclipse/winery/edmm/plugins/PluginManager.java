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

package org.eclipse.winery.edmm.plugins;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.github.edmm.core.plugin.PluginService;
import io.github.edmm.core.plugin.TransformationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);
    private static PluginManager instance;

    private final List<TransformationPlugin<?>> pluginsList = new ArrayList<>();

    private PluginManager() {
        initPlugins();
    }

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public PluginService getPluginService() {
        return new PluginService(pluginsList, new ArrayList<>());
    }

    /**
     * Init the plugins lists with the plugins specified in the pluginContext.xml file in the transformation-framework
     * package
     */
    private void initPlugins() {
        for (String classpath : getPluginsClassPaths()) {
            TransformationPlugin<?> plugin;
            // the plugin are initialized by reflection
            try {
                Class<?> pluginClass = Class.forName(classpath);
                Constructor<?> constructor = pluginClass.getConstructor();
                plugin = (TransformationPlugin<?>) constructor.newInstance();
                pluginsList.add(plugin);
            } catch (Exception e) {
                // just the single plugin won't work
                LOGGER.error("Plugin" + classpath + "initialization failed", e);
            }
        }
    }

    /**
     * Read the pluginContext configuration file
     *
     * @return a list containing the plugins classpaths
     */
    private List<String> getPluginsClassPaths() {
        List<String> pluginsClassPaths = new ArrayList<>();
        Document xmlPluginConfigDocument;

        // opening the file in the edmm.core package
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
        } catch (Exception e) {
            LOGGER.error("Plugin file reading failed", e);
            // returns an empty list, no plugins will be initialized
            return pluginsClassPaths;
        }

        // the plugins are listed with the tag name 'bean'
        NodeList pluginsNodeList = xmlPluginConfigDocument.getElementsByTagName("bean");
        for (int i = 0; i < pluginsNodeList.getLength(); i++) {
            Node plugin = pluginsNodeList.item(i);
            String classpath = plugin.getAttributes().getNamedItem("class").getNodeValue();
            pluginsClassPaths.add(classpath);
        }

        return pluginsClassPaths;
    }

    public List<TransformationPlugin<?>> getPluginsList() {
        return pluginsList;
    }
}
