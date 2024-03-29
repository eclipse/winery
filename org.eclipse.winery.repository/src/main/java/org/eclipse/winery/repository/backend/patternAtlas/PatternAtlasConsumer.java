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

package org.eclipse.winery.repository.backend.patternAtlas;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TPolicyType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods to extract different patterns in the PatternAtlas as NodeType patterns and policy types
 *
 * TODO: For edges: <hostURL>/patternLanguages/<patternLanugageID>/directedEdges
 *                  <hostURL>/patternLanguages/<patternLanugageID>/undirectedEdges
 */
public class PatternAtlasConsumer {
    private static PatternAtlasConsumer instance;
    private static final Logger LOGGER = LoggerFactory.getLogger(PatternAtlasConsumer.class);
    private static final String patternLanguageResourceName = "patternLanguages";
    private static final String patternResourceName = "patterns";
    private static final String patternContentResourceName = "content";
    private static final String patternNamespaceWinery = "http://opentosca.org/nodetypes/patterns";
    private final WebTarget rootTarget;

    private PatternAtlasConsumer(URL patternAtlasRootURL) throws URISyntaxException {
        Client client = ClientBuilder.newClient();
        this.rootTarget = client.target(patternAtlasRootURL.toURI());
    }

    /**
     * @param patternAtlasRootURL URL where the pattern atlas API root is hosted
     * @return Instance of the consumer
     */
    public static PatternAtlasConsumer getInstance(URL patternAtlasRootURL) throws URISyntaxException {
        if (instance == null) {
            instance = new PatternAtlasConsumer(patternAtlasRootURL);
        }
        return instance;
    }

    /**
     * Retrieves a list of simplified PatternLanguages (some fields in the API are omitted)
     *
     * @return All pattern languages contained in the pattern atlas
     */
    public List<PatternLanguage> getPatternLanguages() {
        WebTarget patternLanguageTarget = this.rootTarget.path(patternLanguageResourceName);

        try {
            // Because of the current JSON structure, we manually parse the contents from the _embedded element.
            String jsonResponse = patternLanguageTarget.request().get(String.class);
            JsonNode root = new ObjectMapper().readTree(jsonResponse);
            JsonNode patternLanguages = root.get("_embedded").get("patternLanguageModels");
            List<PatternLanguage> patternLanguageList = new ArrayList<>(patternLanguages.size());
            LOGGER.info("Number of Pattern languages: {}", patternLanguages.size());

            for (JsonNode patternLanguageNode : patternLanguages) {
                patternLanguageList.add(new PatternLanguage(
                    patternLanguageNode.get("id").asText(),
                    patternLanguageNode.get("name").asText(),
                    patternLanguageNode.get("uri").asText(),
                    patternLanguageNode.get("patternCount").asInt()));
            }

            return patternLanguageList;
        } catch (JsonProcessingException | URISyntaxException e) {
            LOGGER.error("Could not synchronize pattern languages with the PatternAtlas!", e);
        } catch (Exception e) {
            // Happens in the case of a java.net.ConnectException.
            // However, as the above block does not throw this Exception, we need to use the generic Exception class.
            LOGGER.error("Could not connect to the PatternAtlas!");
            LOGGER.warn("Continuing without cloning/synchronizing patterns...");
        }

        return Collections.emptyList();
    }

    /**
     * Retrieve all patterns
     *
     * @param patternLanguage The pattern language the
     * @return All patterns in the provided Pattern Language
     */
    public List<Pattern> getPatternsOfPatternLanguage(PatternLanguage patternLanguage) {
        WebTarget patternTarget = this.rootTarget.path(patternLanguageResourceName).path(patternLanguage.id).path(patternResourceName);

        try {
            // Because of the current JSON structure, we manually parse the contents from the _embedded element.
            String jsonResponse = patternTarget.request().get(String.class);
            JsonNode root = new ObjectMapper().readTree(jsonResponse);
            JsonNode patterns = root.get("_embedded").get("patternModels");
            List<Pattern> patternList = new ArrayList<>(patterns.size());
            LOGGER.info("Found {} patterns in pattern language \"{}\"", patterns.size(), patternLanguage.name);

            for (JsonNode patternNode : patterns) {
                patternList.add(new Pattern(
                    patternNode.get("id").asText(),
                    patternNode.get("name").asText(),
                    patternNode.get("uri").asText(),
                    patternNode.get("iconUrl").asText(),
                    patternLanguage,
                    patternNode.get("deploymentModelingBehaviorPattern").asBoolean(),
                    patternNode.get("deploymentModelingStructurePattern").asBoolean()
                ));
            }

            return patternList;
        } catch (JsonProcessingException | URISyntaxException e) {
            LOGGER.error("Could not synchronize patterns with the PatternAtlas!", e);
        }

        return Collections.emptyList();
    }

    public static class PatternLanguage {
        private String id;

        private String name;
        private URI uri;
        private int patternCount;

        private PatternLanguage() {

        }

        public PatternLanguage(String id, String name, String uri, int patternCount) throws URISyntaxException {
            this.id = id;
            this.name = name;
            this.uri = new URI(uri);
            this.patternCount = patternCount;
        }

        @Override
        public String toString() {
            return "PatternLanguage{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", uri=" + uri +
                ", patternCount=" + patternCount +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PatternLanguage that = (PatternLanguage) o;

            if (patternCount != that.patternCount) return false;
            if (!id.equals(that.id)) return false;
            if (!name.equals(that.name)) return false;
            return uri.equals(that.uri);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (uri != null ? uri.hashCode() : 0);
            result = 31 * result + patternCount;
            return result;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public URI getUri() {
            return uri;
        }

        public int getPatternCount() {
            return patternCount;
        }
    }

    // TODO: Maybe we could directly convert it to NodeTypes?
    public static class Pattern {
        private String namespace;
        private String id;
        private String name;
        private URI uri;
        private URL iconURL;
        private String patternLanguageId;
        private String patternLanguageName;
        private boolean deploymentModelingBehaviorPattern;
        private boolean deploymentModelingStructurePattern;
        // From content resource:
        private String intent;
        private String solutionSketch;

        public Pattern(String id, String name, String uri, String iconURL, PatternLanguage patternLanguage,
                       boolean deploymentModelingBehaviorPattern, boolean deploymentModelingStructurePattern) throws URISyntaxException {
            this.id = id;
            // TODO: Use this in default case:
            this.name = name.replace(" ", "-").replace("(", "").replace(")", "");
            this.uri = new URI(uri);
            try {
                this.iconURL = new URL(iconURL);
            } catch (MalformedURLException exception) {
                LOGGER.info("\tNo icon was found for the pattern \"{}\" of pattern language \"{}\".", name, patternLanguage.name);
            }
            this.namespace = patternLanguage.uri.toString();
            this.patternLanguageId = patternLanguage.id;
            this.patternLanguageName = patternLanguage.name;
            this.deploymentModelingBehaviorPattern = deploymentModelingBehaviorPattern;
            this.deploymentModelingStructurePattern = deploymentModelingStructurePattern;
        }

        public TNodeType toTNodeType() {
            TNodeType.Builder nodeTypeBuilder = new TNodeType.Builder(this.name);
            nodeTypeBuilder.setTargetNamespace(this.namespace);
            return nodeTypeBuilder.build();
        }

        public TPolicyType toPolicyType() {
            TPolicyType.Builder policyTypeBuilder = new TPolicyType.Builder(this.name);
            policyTypeBuilder.setTargetNamespace(this.namespace);
            return policyTypeBuilder.build();
        }

        @Override
        public String toString() {
            return "Pattern{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", uri=" + uri +
                ", iconURL=" + iconURL +
                ", patternLanguageId='" + patternLanguageId + '\'' +
                ", patternLanguageName='" + patternLanguageName + '\'' +
                ", intent='" + intent + '\'' +
                ", solutionSketch='" + solutionSketch + '\'' +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pattern pattern = (Pattern) o;

            if (!Objects.equals(id, pattern.id)) return false;
            if (!Objects.equals(name, pattern.name)) return false;
            if (!Objects.equals(uri, pattern.uri)) return false;
            if (!Objects.equals(iconURL, pattern.iconURL)) return false;
            if (!Objects.equals(patternLanguageId, pattern.patternLanguageId))
                return false;
            if (!Objects.equals(patternLanguageName, pattern.patternLanguageName))
                return false;
            if (!Objects.equals(intent, pattern.intent)) return false;
            return Objects.equals(solutionSketch, pattern.solutionSketch);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (uri != null ? uri.hashCode() : 0);
            result = 31 * result + (iconURL != null ? iconURL.hashCode() : 0);
            result = 31 * result + (patternLanguageId != null ? patternLanguageId.hashCode() : 0);
            result = 31 * result + (patternLanguageName != null ? patternLanguageName.hashCode() : 0);
            result = 31 * result + (intent != null ? intent.hashCode() : 0);
            result = 31 * result + (solutionSketch != null ? solutionSketch.hashCode() : 0);
            return result;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public URI getUri() {
            return uri;
        }

        public URL getIconURL() {
            return iconURL;
        }

        public String getPatternLanguageId() {
            return patternLanguageId;
        }

        public String getPatternLanguageName() {
            return patternLanguageName;
        }

        public String getIntent() {
            return intent;
        }

        public String getSolutionSketch() {
            return solutionSketch;
        }

        public boolean getDeploymentModelingBehaviorPattern() {
            return deploymentModelingBehaviorPattern;
        }

        public void setDeploymentModelingBehaviorPattern(boolean deploymentModelingBehaviorPattern) {
            this.deploymentModelingBehaviorPattern = deploymentModelingBehaviorPattern;
        }

        public boolean getDeploymentModelingStructurePattern() {
            return deploymentModelingStructurePattern;
        }

        public void setDeploymentModelingStructurePattern(boolean deploymentModelingStructurePattern) {
            this.deploymentModelingStructurePattern = deploymentModelingStructurePattern;
        }
    }
}
