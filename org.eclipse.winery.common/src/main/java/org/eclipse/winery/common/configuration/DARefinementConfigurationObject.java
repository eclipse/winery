/*******************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.common.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.YAMLConfiguration;

public class DARefinementConfigurationObject extends AbstractConfigurationObject {

    private final String key = "deploymentArtifactRefinements";

    private Map<String, DARefinementService> refinementServices;

    @SuppressWarnings("unused")
    public DARefinementConfigurationObject() {
        if (Environment.getInstance().checkConfigurationForUpdate()) {
            Environment.getInstance().getConfigFromFile();
        }
        this.configuration = Environment.getInstance().getConfiguration();
    }

    public DARefinementConfigurationObject(YAMLConfiguration configuration) {
        this.update(configuration);
    }

    @Override
    void save() {
        this.configuration.setProperty(this.key, this.refinementServices);
        Environment.getInstance().save();
    }

    @Override
    void update(YAMLConfiguration updatedConfiguration) {
        this.configuration = updatedConfiguration;

        this.refinementServices = new HashMap<>();
        this.configuration.getKeys(key).forEachRemaining(yamlKey -> {
            String endpoint = yamlKey.replace(this.key + ".", "").split("\\.")[0];

            DARefinementService daRefinementService = refinementServices.get(endpoint);
            if (daRefinementService == null) {
                daRefinementService = new DARefinementService();
                this.refinementServices.put(endpoint, daRefinementService);
            }

            if (yamlKey.endsWith("url")) {
                daRefinementService.url = this.configuration.getString(yamlKey);
            } else if (yamlKey.endsWith("description")) {
                daRefinementService.description = this.configuration.getString(yamlKey);
            } else if (yamlKey.contains("canRefine")) {
                if (yamlKey.endsWith("from")) {
                    daRefinementService.canRefine.from = this.configuration.getList(String.class, yamlKey);
                } else if (yamlKey.contains("to")) {
                    daRefinementService.canRefine.to = this.configuration.getList(String.class, yamlKey);
                }
            }
        });
    }

    @Override
    void initialize() {
        // there are no defaults
    }

    public Map<String, DARefinementService> getRefinementServices() {
        return refinementServices;
    }

    public void setRefinementServices(Map<String, DARefinementService> refinementServices) {
        this.refinementServices = refinementServices;
    }

    public static class DARefinementService {
        public TransformationCapabilities canRefine = new TransformationCapabilities();
        public String url;
        public String description;
    }

    public static class TransformationCapabilities {
        public List<String> from;
        public List<String> to;
    }
}
