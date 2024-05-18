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

package org.eclipse.winery.crawler.chefcookbooks.kitchenparser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.winery.crawler.chefcookbooks.chefcookbook.CookbookParseResult;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public class ChefKitchenYmlParser {

    private Map<String, Object> kitchenYml;
    private String cookbookName;
    private String cookbookPath;

    public ChefKitchenYmlParser(CookbookParseResult cookbookParseResult) {
        this.cookbookName = cookbookParseResult.getCookbookName();
        this.cookbookPath = cookbookParseResult.getCookbookPath();

        this.kitchenYml = parseKitchen();
    }

    private Map<String, Object> parseKitchen() {

        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);

        Yaml yaml = new Yaml(new SafeConstructor());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(this.cookbookPath + "/kitchen.yml");
        } catch (FileNotFoundException e1) {
            try {
                inputStream = new FileInputStream(this.cookbookPath + "/.kitchen.yml");
            } catch (FileNotFoundException e2) {
                System.err.printf("Cookbook \" " + cookbookName + "\"" + " has no kitchen.yml file");
                return null;
            }
        }

        Map<String, Object> kitchenYml = yaml.load(inputStream);
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return kitchenYml;
    }

    /**
     * @return List with all platform configurations from kitchen.yml each List item is a Map with a platform
     * configuration
     */
    public List getPlatforms() {

        if (this.kitchenYml != null) {
            List<Map> platformConfig = (List) kitchenYml.get("platforms");
            return platformConfig;
        } else return null;
    }

    /**
     * @return List with all platform names from kitchen.yml Platform name includes the platform version
     */
    public List getPlatformNames() {
        List<Map> platformConfig = this.getPlatforms();
        if (platformConfig != null) {
            List platformNames = new ArrayList();
            for (int count = 0; count < platformConfig.size(); count++) {
                platformNames.add(platformConfig.get(count).get("name"));
            }
            return platformNames;
        } else return null;
    }

    /**
     * @return List with all test suites. Each test suite contains a Map with test configurations
     */
    public List getSuites() {
        if (this.kitchenYml != null) {
            List<Map> platformConfig = (List) kitchenYml.get("suites");
            return platformConfig;
        } else return null;
    }
}
