/*******************************************************************************
 * Copyright (c) 2019-2020 Contributors to the Eclipse Foundation
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.winery.common.Constants;

import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryConfigurationObject extends AbstractConfigurationObject {
    // this class shares the responsibility of the Environment class in abstracting 
    // over interactions with the configuration file. Loggers are therefore shared.
    private static final Logger LOGGER = LoggerFactory.getLogger(Environment.class);

    private static final String key = "repository.";
    private GitConfigurationObject gitConfiguration;

    private RepositoryProvider provider;
    private boolean exportNormativeTypes;
    private String repositoryRoot;
    private String csarOutputPath;
    private boolean tenantRepository;

    private YAMLConfiguration configuration;

    RepositoryConfigurationObject(YAMLConfiguration configuration, GitConfigurationObject gitConfigurationObject) {
        this.setGitConfiguration(gitConfigurationObject);
        this.update(configuration);
    }

    public static String getProviderConfigurationKey() {
        return key + "provider";
    }

    @Override
    void save() {
        configuration.setProperty(key + "provider", this.getProvider().toString());
        configuration.setProperty(key + "repositoryRoot", this.repositoryRoot);
        configuration.setProperty(key + "csarOutputPath", this.csarOutputPath);
        configuration.setProperty(key + "tenantMode", this.tenantRepository);
        configuration.setProperty(key + "exportNormativeTypes", this.exportNormativeTypes);
        this.getGitConfiguration().save();
        Environment.getInstance().save();
    }

    @Override
    void update(YAMLConfiguration updatedConfiguration) {
        this.configuration = updatedConfiguration;
        this.repositoryRoot = configuration.getString(key + "repositoryRoot");
        this.csarOutputPath = configuration.getString(key + "csarOutputPath");
        if (this.configuration.containsKey(key + "tenantMode")) {
            this.tenantRepository = configuration.getBoolean(key + "tenantMode");
        }
        if (this.configuration.containsKey(key + "exportNormativeTypes")) {
            this.exportNormativeTypes = configuration.getBoolean(key + "exportNormativeTypes");
        }
        String provider = Environment.getInstance().getConfiguration().getString(getProviderConfigurationKey());
        if (provider.equalsIgnoreCase(RepositoryProvider.YAML.name())) {
            this.setProvider(RepositoryProvider.YAML);
        } else {
            this.setProvider(RepositoryProvider.FILE);
        }
    }

    @Override
    void initialize() {

    }

    public RepositoryConfigurationObject.RepositoryProvider getProvider() {
        return provider;
    }

    public void setProvider(RepositoryProvider provider) {
        this.provider = provider;
    }

    public boolean hasExportNormativeTypes() {
        return this.exportNormativeTypes;
    }
    
    public void setExportNormativeTypes(boolean exportNormativeTypes) {
        this.exportNormativeTypes = exportNormativeTypes;
        this.save();
    }

    /**
     * Returns the path to the repository saved in the configuration file.
     *
     * @return path to configuration
     */
    public String getRepositoryRoot() {
        String repositoryRoot = this.repositoryRoot;
        if (repositoryRoot == null || repositoryRoot.isEmpty()) {
            repositoryRoot = determineAndCreateRepositoryPath().toString();
        }
        setRepositoryRoot(repositoryRoot);
        return repositoryRoot;
    }

    public void setRepositoryRoot(String changedRepositoryRoot) {
        this.repositoryRoot = changedRepositoryRoot;
        this.save();
    }

    public String getCsarOutputPath() {
        String csarOutputPath = this.csarOutputPath;
        if (csarOutputPath == null || csarOutputPath.isEmpty()) {
            csarOutputPath = getRepositoryRoot() + File.separator + "csars";
        }
        setCsarOutputPath(csarOutputPath);
        createCsarOutputPath(csarOutputPath);
        return csarOutputPath;
    }

    public void setCsarOutputPath(String csarOutputPath) {
        this.csarOutputPath = csarOutputPath;
        this.save();
    }

    public GitConfigurationObject getGitConfiguration() {
        return gitConfiguration;
    }

    public void setGitConfiguration(GitConfigurationObject gitConfiguration) {
        this.gitConfiguration = gitConfiguration;
    }

    public void setTenantRepository(boolean tenantRepository) {
        this.tenantRepository = tenantRepository;
        this.save();
    }

    public boolean isTenantRepository() {
        return tenantRepository;
    }

    private static Path determineAndCreateRepositoryPath() {
        Path repositoryPath;
        if (SystemUtils.IS_OS_WINDOWS) {
            if (Files.exists(Constants.GLOBAL_REPO_PATH_WINDOWS)) {
                repositoryPath = Constants.GLOBAL_REPO_PATH_WINDOWS;
            } else {
                repositoryPath = createDefaultRepositoryPath();
            }
        } else {
            repositoryPath = createDefaultRepositoryPath();
        }
        return repositoryPath;
    }

    private static Path createDefaultRepositoryPath() {
        File repo = null;
        boolean operationalFileSystemAccess;
        try {
            repo = new File(FileUtils.getUserDirectory(), Constants.DEFAULT_REPO_NAME);
            operationalFileSystemAccess = true;
        } catch (NullPointerException e) {
            // it seems, we run at a system, where we do not have any filesystem
            // access
            operationalFileSystemAccess = false;
        }

        // operationalFileSystemAccess = false;

        Path repositoryPath;
        if (operationalFileSystemAccess) {
            try {
                FileUtils.forceMkdir(repo);
            } catch (IOException e) {
                LOGGER.debug("Error while creating directory.", e);
            }
            repositoryPath = repo.toPath();
        } else {
            // we do not have access to the file system
            throw new IllegalStateException("No write access to file system");
        }

        return repositoryPath;
    }

    private static void createCsarOutputPath(String csarOutputPath) {
        File outputPath = new File(csarOutputPath);
        if (outputPath.exists() && outputPath.isDirectory()) {
            return;
        }
        try {
            org.apache.commons.io.FileUtils.forceMkdir(outputPath);
        } catch (IOException e) {
            LOGGER.error("Error while creating directory: {}", e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    public enum RepositoryProvider {

        FILE("file"), YAML("yaml");

        private final String name;

        RepositoryProvider(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
