/*******************************************************************************
 * Copyright (c) 2019-2022 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.tosca.utils;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.winery.model.tosca.DeploymentTechnologyDescriptor;
import org.eclipse.winery.model.tosca.HasId;
import org.eclipse.winery.model.tosca.RelationshipSourceOrTarget;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TCapabilityDefinition;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TPlan;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.model.tosca.constants.QNames;
import org.eclipse.winery.model.tosca.constants.ToscaBaseTypes;
import org.eclipse.winery.model.tosca.extensions.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class ModelUtilities {

    public static final QName QNAME_LOCATION = new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "location");
    public static final QName QNAME_PARTICIPANT = new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE,
        "participant");
    public static final QName NODE_TEMPLATE_REGION = new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "region");
    public static final QName NODE_TEMPLATE_PROVIDER = new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE,
        "provider");
    public static final QName RELATIONSHIP_TEMPLATE_TRANSFER_TYPE =
        new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "dataTransferType");
    public static final String TAG_DEPLOYMENT_TECHNOLOGIES = "jsonDeploymentTechnologies";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ModelUtilities.class);
    private static final DocumentBuilder DOCUMENT_BUILDER;

    static {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String FEATURE = null;
        try {
            FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            dbf.setFeature(FEATURE, false);

            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            dbf.setFeature(FEATURE, false);

            FEATURE = "http://xml.org/sax/features/external-general-entities";
            dbf.setFeature(FEATURE, false);

            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("The feature '"
                + FEATURE + "' is not supported by your XML processor.", e);
        }
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Could not initialize document builder", e);
            throw new IllegalStateException("Could not initialize document builder", e);
        }
        DOCUMENT_BUILDER = documentBuilder;
    }

    /**
     * This is a special method for Winery. Winery allows to define a property by specifying name (or value) values.
     * Instead of parsing the XML contained in TNodeType, this method is a convenience method to access this
     * information
     * <p>
     * The return type "Properties" is used because of the key/value properties.
     *
     * @param template the node template to get the associated properties
     */
    public static LinkedHashMap<String, String> getPropertiesKV(TEntityTemplate template) {
        if (template.getProperties() instanceof TEntityTemplate.WineryKVProperties) {
            return ((TEntityTemplate.WineryKVProperties) template.getProperties()).getKVProperties();
        } else if (template.getProperties() instanceof TEntityTemplate.YamlProperties) {
            // flattening YAML properties here
            return ((TEntityTemplate.YamlProperties) template.getProperties()).getProperties()
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, String::valueOf, (s, s2) -> s, LinkedHashMap::new));
        } else {
            return null;
        }
    }

    public static void setPropertiesKV(TEntityTemplate template, LinkedHashMap<String, String> properties) {
        if (template.getProperties() == null) {
            template.setProperties(new TEntityTemplate.WineryKVProperties());
        }
        if (template.getProperties() instanceof TEntityTemplate.WineryKVProperties) {
            ((TEntityTemplate.WineryKVProperties) template.getProperties()).setKVProperties(properties);
        }
    }

    public static boolean hasKvProperties(TEntityTemplate entityTemplate) {
        return Objects.nonNull(entityTemplate.getProperties())
            && Objects.nonNull(ModelUtilities.getPropertiesKV(entityTemplate));
    }

    /**
     * Generates a XSD when Winery's K/V properties are used. This method is put here instead of
     * WinerysPropertiesDefinitionResource to avoid generating the subresource
     * <p>
     * public because of the usage by TOSCAExportUtil
     *
     * @return empty Document, if Winery's Properties Definition is not fully filled (e.g., no wrapping element defined)
     */
    public static Document getWinerysPropertiesDefinitionXsdAsDocument(WinerysPropertiesDefinition wpd) {
        /*
         * This is a quick hack: an XML schema container is created for each
         * element. Smarter solution: create a hash from namespace to XML schema
         * element and re-use that for each new element
         * Drawback of "smarter" solution: not a single XSD file anymore
         */
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        String FEATURE = null;
        try {
            FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            docFactory.setFeature(FEATURE, false);

            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            docFactory.setFeature(FEATURE, false);

            FEATURE = "http://xml.org/sax/features/external-general-entities";
            docFactory.setFeature(FEATURE, false);

            docFactory.setXIncludeAware(false);
            docFactory.setExpandEntityReferences(false);

            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("The feature '"
                + FEATURE + "' is not supported by your XML processor.", e);
        }
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            ModelUtilities.LOGGER.debug(e.getMessage(), e);
            throw new IllegalStateException("Could not instantiate document builder", e);
        }
        Document doc = docBuilder.newDocument();

        if (!ModelUtilities.allRequiredFieldsNonNull(wpd)) {
            // wpd not fully filled -> valid XSD cannot be provided
            // fallback: add comment and return "empty" document
            Comment comment = doc.createComment("Required fields are missing in Winery's key/value properties definition.");
            doc.appendChild(comment);
            return doc;
        }

        // create XSD schema container
        Element schemaElement = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "schema");
        doc.appendChild(schemaElement);
        schemaElement.setAttribute("elementFormDefault", "qualified");
        schemaElement.setAttribute("attributeFormDefault", "unqualified");
        schemaElement.setAttribute("targetNamespace", wpd.getNamespace());

        // create XSD element itself
        Element el = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
        schemaElement.appendChild(el);
        el.setAttribute("name", wpd.getElementName());
        Element el2 = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "complexType");
        el.appendChild(el2);
        el = el2;
        el2 = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "sequence");
        el.appendChild(el2);
        el = el2;

        // currently, "xsd" is a hardcoded prefix in the type definition
        el.setAttribute("xmlns:xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI);

        for (PropertyDefinitionKV prop : wpd.getPropertyDefinitions()) {
            el2 = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
            el.appendChild(el2);
            el2.setAttribute("name", prop.getKey());
            // prop.getType has the prefix included
            el2.setAttribute("type", prop.getType());
        }

        return doc;
    }

    /**
     * Removes an existing Winery's Properties definition. If no such definition exists, the TEntityType is not
     * modified
     */
    public static void removeWinerysPropertiesDefinition(TEntityType et) {
        for (Iterator<Object> iterator = et.getAny().iterator(); iterator.hasNext(); ) {
            Object o = iterator.next();
            if (o instanceof WinerysPropertiesDefinition) {
                iterator.remove();
                break;
            }
        }
    }

    public static void replaceWinerysPropertiesDefinition(TEntityType et, WinerysPropertiesDefinition wpd) {
        ModelUtilities.removeWinerysPropertiesDefinition(et);
        et.getAny().add(wpd);
    }

    /**
     * Determines a color belonging to the given name
     */
    public static String getColor(String name) {
        int hash = name.hashCode();
        // trim to 3*8=24 bits
        hash = hash & 0xFFFFFF;
        // check if color is more than #F0F0F0, i.e., too light
        if (((hash & 0xF00000) >= 0xF00000) && (((hash & 0x00F000) >= 0x00F000) && ((hash & 0x0000F0) >= 0x0000F0))) {
            // set one high bit to zero for each channel. That makes the overall color darker
            hash = hash & 0xEFEFEF;
        }
        return String.format("#%06x", hash);
    }

    public static String getBorderColor(TNodeType nt) {
        String borderColor = nt.getOtherAttributes().get(QNames.QNAME_BORDER_COLOR);
        if (borderColor == null) {
            borderColor = getColor(nt.getName() == null ? "" : nt.getName());
        }
        return borderColor;
    }

    public static String getColor(TRelationshipType rt) {
        String color = rt.getOtherAttributes().get(QNames.QNAME_COLOR);
        if (color == null) {
            color = getColor(rt.getName() == null ? "" : rt.getName());
        }
        return color;
    }

    /**
     * Returns the Properties. If no properties exist, the element is created
     */
    public static org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties getProperties(TBoundaryDefinitions defs) {
        org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties properties = defs.getProperties();
        if (properties == null) {
            properties = new org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties();
            defs.setProperties(properties);
        }
        return properties;
    }

    /**
     * Special method to get the name of an extensible element as the TOSCA specification does not have a separate super
     * type for elements with a name
     * <p>
     *
     * @param e the extensible element offering a name attribute (besides an id attribute)
     * @return the name of the extensible element
     * @throws IllegalStateException if e does not offer the method "getName"
     */
    public static String getName(TExtensibleElements e) {
        Method method;
        Object res;
        try {
            method = e.getClass().getMethod("getName");
            res = method.invoke(e);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return (String) res;
    }

    /**
     * Returns the name of the given element. If the name does not exist or is empty, the id is returned
     * <p>
     * {@link #getName}
     *
     * @return the name if there is a name field, if not, the id is returned. In case there is a Name field,
     */
    public static String getNameWithIdFallBack(TExtensibleElements ci) {
        Method method;
        String res = null;
        //noinspection EmptyCatchBlock
        try {
            method = ci.getClass().getMethod("getName");
            res = (String) method.invoke(ci);
        } catch (Exception e) {
        }
        if (res == null) {
            try {
                method = ci.getClass().getMethod("getId");
                res = (String) method.invoke(ci);
            } catch (Exception e2) {
                throw new IllegalStateException(e2);
            }
        }
        return res;
    }

    /**
     * Special method to set the name of an extensible element as the TOSCA specification does not have a separate super
     * type for elements with a name
     *
     * @param e    the extensible element offering a name attribute (besides an id attribute)
     * @param name the new name
     * @throws IllegalStateException if e does not offer the method "getName"
     */
    public static void setName(TExtensibleElements e, String name) {
        Method method;
        try {
            method = e.getClass().getMethod("setName", String.class);
            method.invoke(e, name);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static boolean allRequiredFieldsNonNull(WinerysPropertiesDefinition wpd) {
        boolean valid = wpd.getNamespace() != null;
        valid = valid && (wpd.getElementName() != null);
        if (valid) {
            List<PropertyDefinitionKV> propertyDefinitions = wpd.getPropertyDefinitions();
            valid = (propertyDefinitions != null);
            if (valid) {
                for (PropertyDefinitionKV def : propertyDefinitions) {
                    valid = valid && (def.getKey() != null);
                    valid = valid && (def.getType() != null);
                }
            }
        }
        return valid;
    }

    public static Optional<Integer> getLeft(TNodeTemplate nodeTemplate) {
        Map<QName, String> otherAttributes = nodeTemplate.getOtherAttributes();
        String x = otherAttributes.get(new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "x"));
        if (x == null) {
            return Optional.empty();
        }
        float floatValue;
        try {
            floatValue = Float.parseFloat(x);
        } catch (NumberFormatException e) {
            LOGGER.debug("Could not parse x value", e);
            return Optional.empty();
        }
        return Optional.of((int) floatValue);
    }

    /**
     * @return null if no explicit left is set
     */
    public static String getTop(TNodeTemplate nodeTemplate) {
        Map<QName, String> otherAttributes = nodeTemplate.getOtherAttributes();
        return otherAttributes.get(new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "y"));
    }

    /**
     * locates targetObjectRef inside a topology template
     *
     * @param topologyTemplate the topology template to search in
     * @param targetObjectRef  the object ref as String
     * @return null if not found, otherwise the entity template in the topology
     */
    public static TEntityTemplate findNodeTemplateOrRequirementOfNodeTemplateOrCapabilityOfNodeTemplateOrRelationshipTemplate(TTopologyTemplate topologyTemplate, String targetObjectRef) {
        // We cannot use XMLs id pointing capabilities as we work on the Java model
        // Other option: modify the stored XML directly. This is more error prune than walking through the whole topology
        for (TEntityTemplate t : topologyTemplate.getNodeTemplateOrRelationshipTemplate()) {
            if (t.getId().equals(targetObjectRef)) {
                return t;
            }
            if (t instanceof TNodeTemplate) {
                TNodeTemplate nt = (TNodeTemplate) t;

                TRequirement req = resolveRequirement(targetObjectRef, nt);
                if (req != null) {
                    return req;
                }

                TCapability cap = resolveCapability(targetObjectRef, nt);
                if (cap != null) {
                    return cap;
                }
            }
        }

        // no return hit inside the loop: nothing was found
        return null;
    }

    /**
     * Returns the id of the given element
     * <p>
     * The TOSCA specification does NOT always put an id field. In the case of EntityTypes and
     * EntityTypeImplementations, there is no id, but a name field
     * <p>
     * This method abstracts from that fact.
     */
    public static String getId(TExtensibleElements ci) {
        Method method;
        Object res;
        try {
            method = ci.getClass().getMethod("getId");
            res = method.invoke(ci);
        } catch (Exception e) {
            // If no "getId" method is there, we try "getName"
            try {
                method = ci.getClass().getMethod("getName");
                res = method.invoke(ci);
            } catch (Exception e2) {
                throw new IllegalStateException(e2);
            }
        }
        return (String) res;
    }

    /**
     * Resolves a given id as requirement in the given ServiceTemplate
     *
     * @return null if not found
     */
    public static TRequirement resolveRequirement(TServiceTemplate serviceTemplate, String referenceId) {
        if (serviceTemplate.getTopologyTemplate() != null) {
            for (TNodeTemplate nt : serviceTemplate.getTopologyTemplate().getNodeTemplates()) {
                TRequirement req = resolveRequirement(referenceId, nt);
                if (req != null) {
                    return req;
                }
            }
        }
        return null;
    }

    private static TRequirement resolveRequirement(String reference, TNodeTemplate nt) {
        List<TRequirement> requirements = nt.getRequirements();
        if (requirements != null) {
            for (TRequirement req : requirements) {
                if (req.getId().equals(reference)) {
                    return req;
                }
            }
        }
        return null;
    }

    public static TCapability resolveCapability(TServiceTemplate serviceTemplate, String referenceId) {
        if (serviceTemplate.getTopologyTemplate() != null) {
            for (TNodeTemplate nt : serviceTemplate.getTopologyTemplate().getNodeTemplates()) {
                TCapability cap = resolveCapability(referenceId, nt);
                if (cap != null) {
                    return cap;
                }
            }
        }
        return null;
    }

    private static TCapability resolveCapability(String referenceId, TNodeTemplate nt) {
        List<TCapability> capabilities = nt.getCapabilities();
        if (capabilities != null) {
            for (TCapability cap : capabilities) {
                if (cap.getId().equals(referenceId)) {
                    return cap;
                }
            }
        }
        return null;
    }

    public static TNodeTemplate resolveNodeTemplate(TServiceTemplate serviceTemplate, String reference) {
        if (serviceTemplate.getTopologyTemplate() != null) {
            for (TNodeTemplate nodeTemplate : serviceTemplate.getTopologyTemplate().getNodeTemplates()) {
                if (nodeTemplate.getId().equals(reference)) {
                    return nodeTemplate;
                }
            }
        }
        return null;
    }

    public static TRelationshipTemplate resolveRelationshipTemplate(TServiceTemplate serviceTemplate, String reference) {
        if (serviceTemplate.getTopologyTemplate() != null) {
            for (TEntityTemplate tmpl : serviceTemplate.getTopologyTemplate().getNodeTemplateOrRelationshipTemplate()) {
                if (tmpl instanceof TRelationshipTemplate) {
                    TRelationshipTemplate relationshipTemplate = (TRelationshipTemplate) tmpl;
                    if (relationshipTemplate.getId().equals(reference)) {
                        return relationshipTemplate;
                    }
                }
            }
        }
        return null;
    }

    public static TPlan resolvePlan(TServiceTemplate serviceTemplate, String reference) {
        List<TPlan> plans = serviceTemplate.getPlans();
        if (plans != null) {
            for (TPlan p : plans) {
                if (p.getId().equals(reference)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * This method instantiates a {@link TNodeTemplate} for a given {@link TNodeType}.
     *
     * @param nodeType the {@link TNodeType} used for the {@link TNodeTemplate} instantiation.
     * @return the instantiated {@link TNodeTemplate}
     */
    public static TNodeTemplate instantiateNodeTemplate(TNodeType nodeType) {
        String nodeTemplateId = nodeType.getIdFromIdOrNameField() + Math.random();
        TNodeTemplate.Builder builder = new TNodeTemplate.Builder(nodeTemplateId, nodeType.getQName());
        builder.setName(nodeType.getName());

        // add capabilities to the NodeTemplate
        if (nodeType.getCapabilityDefinitions() != null) {
            for (TCapabilityDefinition cd : nodeType.getCapabilityDefinitions()) {
                TCapability capability = new TCapability.Builder(
                    cd.getName() + nodeTemplateId,
                    cd.getCapabilityType(),
                    cd.getName()
                ).build();
                builder.addCapability(capability);
            }
        }

        // add requirements
        if (nodeType.getRequirementDefinitions() != null) {
            nodeType.getRequirementDefinitions().forEach(tRequirementDefinition -> {
                    TRequirement requirement = new TRequirement.Builder(
                        tRequirementDefinition.getName() + nodeTemplateId,
                        tRequirementDefinition.getName(),
                        tRequirementDefinition.getRequirementType()
                    ).build();
                    builder.addRequirement(requirement);
                }
            );
        }

        // add properties
        WinerysPropertiesDefinition propDef = nodeType.getWinerysPropertiesDefinition();
        if (propDef != null && propDef.getPropertyDefinitions() != null) {
            Map<String, String> properties = new HashMap<>();
            propDef.getPropertyDefinitions().forEach(propertyDefinition -> {
                    if (propertyDefinition.getDefaultValue() != null)
                        properties.put(propertyDefinition.getKey(), propertyDefinition.getDefaultValue());
                    else {
                        properties.put(propertyDefinition.getKey(), "");
                    }
                }

            );
            TEntityTemplate.WineryKVProperties tProps = new TEntityTemplate.WineryKVProperties();
            tProps.setNamespace(propDef.getNamespace());
            tProps.setElementName(propDef.getElementName());
            tProps.setKVProperties(new LinkedHashMap<>(properties));
            builder.setProperties(tProps);
        }

        return builder.build();
    }

    /**
     * This method instantiates a {@link TRelationshipTemplate} for a given {@link TRelationshipType}.
     *
     * @param relationshipType   the {@link TRelationshipType} used for the {@link TRelationshipTemplate}
     *                           instantiation.
     * @param sourceNodeTemplate the source {@link TNodeTemplate} of the connection
     * @param targetNodeTemplate the target {@link TNodeTemplate} of the connection
     * @return the instantiated {@link TRelationshipTemplate}
     */
    public static TRelationshipTemplate instantiateRelationshipTemplate(TRelationshipType relationshipType,
                                                                        TNodeTemplate sourceNodeTemplate,
                                                                        TNodeTemplate targetNodeTemplate) {
        if (relationshipType == null || relationshipType.getName() == null) {
            return null;
        }

        return new TRelationshipTemplate.Builder(
            "con-" + UUID.randomUUID(),
            new QName(relationshipType.getTargetNamespace(), relationshipType.getName()),
            sourceNodeTemplate,
            targetNodeTemplate
        )
            .setName(relationshipType.getName())
            .build();
    }

    /**
     * Target label is not present if - empty string - undefined - null Target Label is not case-sensitive -> always
     * lower case.
     */
    public static Optional<String> getTargetLabel(TNodeTemplate nodeTemplate) {
        return getOtherAttributeValue(nodeTemplate, QNAME_LOCATION);
    }

    public static Optional<String> getParticipant(TNodeTemplate nodeTemplate) {
        return getOtherAttributeValue(nodeTemplate, QNAME_PARTICIPANT);
    }

    public static void setParticipant(TNodeTemplate nodeTemplate, String participant) {
        Objects.requireNonNull(nodeTemplate);
        Objects.requireNonNull(participant);
        Map<QName, String> otherAttributes = nodeTemplate.getOtherAttributes();
        otherAttributes.put(QNAME_PARTICIPANT, participant);
    }

    public static List<TNodeTemplate> getNodeTemplatesOfParticipant(String participantName, List<TNodeTemplate> nodeTemplates) {

        return nodeTemplates.stream().filter(nt -> getParticipant(nt).isPresent())
            .filter(nt -> getParticipant(nt).get().equals(participantName)).collect(Collectors.toList());
    }

    public static String getOwnerParticipantOfServiceTemplate(TServiceTemplate serviceTemplate) {
        if (serviceTemplate.getTags() != null &&
            serviceTemplate.getTags().stream().anyMatch(t -> t.getName().equals("participant"))) {
            return serviceTemplate.getTags().stream().filter(t -> t.getName().equals("participant")).findFirst().get().getValue();
        }
        return null;
    }

    private static Optional<String> getOtherAttributeValue(TNodeTemplate nodeTemplate, QName otherAttribute) {
        if (nodeTemplate == null) {
            return Optional.empty();
        }
        Map<QName, String> otherAttributes = nodeTemplate.getOtherAttributes();
        String participant = otherAttributes.get(otherAttribute);
        if (participant != null && (participant.equals("undefined") || participant.equals(""))) {
            return Optional.empty();
        }
        return Optional.ofNullable(participant).map(String::toLowerCase);
    }

    /**
     * Target Label is not case-sensitive -> set to lowercase.
     */
    public static void setTargetLabel(TNodeTemplate nodeTemplate, String targetLabel) {
        Objects.requireNonNull(nodeTemplate);
        Objects.requireNonNull(targetLabel);
        Map<QName, String> otherAttributes = nodeTemplate.getOtherAttributes();
        otherAttributes.put(QNAME_LOCATION, targetLabel.toLowerCase());
    }

    public static TNodeTemplate getSourceNodeTemplateOfRelationshipTemplate(TTopologyTemplate
                                                                                topologyTemplate, TRelationshipTemplate relationshipTemplate) {
        if (relationshipTemplate.getSourceElement().getRef() instanceof TRequirement) {
            TRequirement requirement = (TRequirement) relationshipTemplate.getSourceElement().getRef();
            return topologyTemplate.getNodeTemplates().stream()
                .filter(nt -> nt.getRequirements() != null)
                .filter(nt -> nt.getRequirements().contains(requirement))
                .findAny()
                .orElse(null);
        } else {
            return (TNodeTemplate) relationshipTemplate.getSourceElement().getRef();
        }
    }

    public static TNodeTemplate getTargetNodeTemplateOfRelationshipTemplate(TTopologyTemplate topologyTemplate,
                                                                            TRelationshipTemplate relationshipTemplate) {
        if (relationshipTemplate.getTargetElement().getRef() instanceof TCapability) {
            TCapability capability = (TCapability) relationshipTemplate.getTargetElement().getRef();
            return topologyTemplate.getNodeTemplates().stream()
                .filter(nt -> nt.getCapabilities() != null)
                .filter(nt -> nt.getCapabilities().contains(capability))
                .findAny()
                .orElse(null);
        }

        return (TNodeTemplate) relationshipTemplate.getTargetElement().getRef();
    }

    /**
     * @return incoming relationship templates <em>pointing to node templates</em>
     */
    public static List<TRelationshipTemplate> getIncomingRelationshipTemplates(TTopologyTemplate topologyTemplate,
                                                                               TNodeTemplate nodeTemplate) {
        Objects.requireNonNull(topologyTemplate);
        Objects.requireNonNull(nodeTemplate);

        return topologyTemplate.getRelationshipTemplates()
            .stream()
            .filter(rt -> getTargetNodeTemplateOfRelationshipTemplate(topologyTemplate, rt).equals(nodeTemplate))
            .collect(Collectors.toList());
    }

    /**
     * @return outgoing relationship templates <em>pointing to node templates</em>
     */
    public static List<TRelationshipTemplate> getOutgoingRelationshipTemplates(TTopologyTemplate topologyTemplate,
                                                                               TNodeTemplate nodeTemplate) {
        Objects.requireNonNull(topologyTemplate);
        Objects.requireNonNull(nodeTemplate);

        return topologyTemplate.getRelationshipTemplates()
            .stream()
            .filter(rt -> getSourceNodeTemplateOfRelationshipTemplate(topologyTemplate, rt).equals(nodeTemplate))
            .collect(Collectors.toList());
    }

    /**
     * @return all nodes templates of the topologyTemplate
     * @deprecated Use {@link TTopologyTemplate#getNodeTemplates()}
     */
    @Deprecated
    public static List<TNodeTemplate> getAllNodeTemplates(TTopologyTemplate topologyTemplate) {
        Objects.requireNonNull(topologyTemplate);

        return topologyTemplate.getNodeTemplateOrRelationshipTemplate()
            .stream()
            .filter(x -> x instanceof TNodeTemplate)
            .map(TNodeTemplate.class::cast)
            .collect(Collectors.toList());
    }

    /**
     * @deprecated Use {@link TTopologyTemplate#getRelationshipTemplates()}
     */
    @Deprecated
    public static List<TRelationshipTemplate> getAllRelationshipTemplates(TTopologyTemplate topologyTemplate) {
        Objects.requireNonNull(topologyTemplate);

        return topologyTemplate.getNodeTemplateOrRelationshipTemplate()
            .stream()
            .filter(x -> x instanceof TRelationshipTemplate)
            .map(TRelationshipTemplate.class::cast)
            .collect(Collectors.toList());
    }

    /**
     * When sending JSON to the server, the content of "any" is a String and not some JSON data structure. To be able to
     * save it as XML, we have to "objectize" the content of Any
     *
     * @param templates The templates (node, relationship) to update. The content of the given collection is modified.
     * @throws IllegalStateException if DocumentBuilder could not initialize
     * @throws IOException           if something goes wrong during parsing
     */
    public static void patchAnyAttributes(Collection<? extends TEntityTemplate> templates) throws IOException {
        Map<QName, String> tempConvertedOtherAttributes = new HashMap<>();

        for (TEntityTemplate template : templates) {
            //Convert the wrong QName created by the JSON serialization back to a right QName
            for (Map.Entry<QName, String> otherAttribute : template.getOtherAttributes().entrySet()) {
                QName qName;
                String localPart = otherAttribute.getKey().getLocalPart();
                if (localPart.startsWith("{")) {
                    // QName is stored as plain string - this is the case when nested in "any"
                    qName = QName.valueOf(localPart);
                } else {
                    // sometimes, the QName is retrieved properly. So, we just keep it.
                    // This is the case when directly nested in nodetemplate JSON.
                    qName = new QName(otherAttribute.getKey().getNamespaceURI(), localPart);
                }
                tempConvertedOtherAttributes.put(qName, otherAttribute.getValue());
            }
            template.getOtherAttributes().clear();
            template.getOtherAttributes().putAll(tempConvertedOtherAttributes);
            tempConvertedOtherAttributes.clear();

            // Convert the String created by the JSON serialization back to an XML dom document
            TEntityTemplate.Properties properties = template.getProperties();
            if (properties instanceof TEntityTemplate.XmlProperties) {
                TEntityTemplate.XmlProperties props = (TEntityTemplate.XmlProperties) properties;
                props.setAny(patchAnyItem(props.getAny()));
            }
        }
    }

    public static Object patchAnyItem(Object item) throws IOException {
        if (item == null) {
            return null;
        }
        if (item instanceof String) {
            Document doc;
            try {
                doc = DOCUMENT_BUILDER.parse(new InputSource(new StringReader((String) item)));
            } catch (SAXException e) {
                LOGGER.error("Could not parse", e);
                throw new IOException("Could not parse", e);
            } catch (IOException e) {
                LOGGER.error("Could not parse", e);
                throw e;
            }
            return doc.getDocumentElement();
        }
        if (!(item instanceof Element)) {
            LOGGER.warn("any item to be converter was not an XML-String or an Element, but of unexpected type {}!", item.getClass().getName());
            // TODO: consider implications of this, if it ever happens
        }
        return item;
    }

    public static boolean isOfType(QName requiredType, QName givenType, Map<QName, ? extends TEntityType> elements) {
        if (!givenType.equals(requiredType)) {
            TEntityType entityType = elements.get(givenType);
            if (Objects.isNull(entityType) || Objects.isNull(entityType.getDerivedFrom())) {
                return false;
            } else {
                return isOfType(requiredType, entityType.getDerivedFrom().getTypeAsQName(), elements);
            }
        }
        return true;
    }

    public static <T extends TEntityType> Map<QName, T> getChildrenOf(QName givenType, Map<QName, T> elements) {
        HashMap<QName, T> children = new HashMap<>();
        TEntityType entityType = elements.get(givenType);
        if (Objects.nonNull(entityType)) {
            elements.forEach((qName, type) -> {
                if (isOfType(givenType, qName, elements)) {
                    children.put(qName, type);
                }
            });
        }
        return children;
    }

    public static <T extends TEntityType> Map<QName, T> getDirectChildrenOf(QName givenType, Map<QName, T> elements) {
        HashMap<QName, T> children = new HashMap<>();
        TEntityType entityType = elements.get(givenType);
        if (Objects.nonNull(entityType)) {
            elements.forEach((qName, type) -> {
                if (!qName.equals(givenType) && type.getDerivedFrom() != null && type.getDerivedFrom().getType().equals(givenType)) {
                    children.put(qName, type);
                }
            });
        }
        return children;
    }

    public static <T extends TEntityType> boolean isFeatureType(QName givenType, Map<QName, T> elements) {
        return Objects.nonNull(elements.get(givenType))
            && Objects.nonNull(elements.get(givenType).getTags())
            && elements.get(givenType).getTags().stream()
            .anyMatch(tag -> "feature".equals(tag.getName()));
    }

    public static void updateNodeTemplate(TTopologyTemplate topology, String oldComponentId, QName
        newType, TNodeType newComponentType) {
        TNodeTemplate nodeTemplate = topology.getNodeTemplate(oldComponentId);

        if (nodeTemplate != null) {
            nodeTemplate.setType(newType);
            nodeTemplate.setName(newType.getLocalPart());
            // TODO: also make some more adjustments etc.
        }
    }

    /**
     * This is specific to the TOSCA hostedOn relationship type.
     */
    public static ArrayList<TNodeTemplate> getHostedOnSuccessors(TTopologyTemplate topologyTemplate, String nodeTemplate) {
        return getHostedOnSuccessors(topologyTemplate, topologyTemplate.getNodeTemplate(nodeTemplate));
    }

    public static ArrayList<TNodeTemplate> getHostedOnSuccessors(TTopologyTemplate topologyTemplate, TNodeTemplate nodeTemplate) {
        ArrayList<TNodeTemplate> hostedOnSuccessors = new ArrayList<>();

        Optional<TRelationshipTemplate> hostedOn;

        do {
            List<TRelationshipTemplate> outgoingRelationshipTemplates = getOutgoingRelationshipTemplates(topologyTemplate, nodeTemplate);

            hostedOn = outgoingRelationshipTemplates.stream()
                .filter(relation -> relation.getType().equals(ToscaBaseTypes.hostedOnRelationshipType))
                .findFirst();
            if (hostedOn.isPresent()) {
                nodeTemplate = getNodeTemplateFromRelationshipSourceOrTarget(topologyTemplate, hostedOn.get().getTargetElement().getRef());
                hostedOnSuccessors.add(nodeTemplate);
            }
        } while (hostedOn.isPresent());

        return hostedOnSuccessors;
    }

    public static ArrayList<TNodeTemplate> getHostedOnPredecessors(TTopologyTemplate topologyTemplate, TNodeTemplate nodeTemplate) {
        ArrayList<TNodeTemplate> hostedOnPredecessors = new ArrayList<>();

        Stack<TNodeTemplate> unprocessed = new Stack<>();
        unprocessed.push(nodeTemplate);

        do {
            List<TRelationshipTemplate> incomingRelationshipTemplates = getIncomingRelationshipTemplates(topologyTemplate, unprocessed.pop());

            incomingRelationshipTemplates.stream()
                .filter(relation -> relation.getType().equals(ToscaBaseTypes.hostedOnRelationshipType))
                .map(hostedOn -> getNodeTemplateFromRelationshipSourceOrTarget(topologyTemplate, hostedOn.getSourceElement().getRef()))
                .forEach(node -> {
                    unprocessed.push(node);
                    hostedOnPredecessors.add(node);
                });
        } while (!unprocessed.isEmpty());

        return hostedOnPredecessors;
    }

    /**
     * Returns the referenced TNodeTemplate of a TRelationshipTemplate which internally uses a
     * RelationshipSourceOrTarget element. to point to the respective TNodeTemplate. If the referenced TNodeTemplate
     * cannot be found, a NullPointerException is thrown.
     *
     * @param topologyTemplate           the TTopologyTemplate the TNodeTemplate and TRelationshipTemplate are contained
     *                                   in.
     * @param relationshipSourceOrTarget the source or target element the relationship points to.
     * @return the actual TNodeTemplate the TRelationshipTemplate is referring to.
     */
    public static TNodeTemplate getNodeTemplateFromRelationshipSourceOrTarget(TTopologyTemplate topologyTemplate,
                                                                              RelationshipSourceOrTarget relationshipSourceOrTarget) {
        Optional<TNodeTemplate> nodeTemplate = Optional.empty();

        if (relationshipSourceOrTarget instanceof TNodeTemplate) {
            nodeTemplate = Optional.of((TNodeTemplate) relationshipSourceOrTarget);
        } else if (relationshipSourceOrTarget instanceof TCapability) {
            nodeTemplate = topologyTemplate.getNodeTemplates().stream()
                .filter(node -> Objects.nonNull(node.getCapabilities()))
                .filter(node ->
                    node.getCapabilities().stream()
                        .anyMatch(capability -> capability.getId().equals(relationshipSourceOrTarget.getId()))
                ).findFirst();
        } else if (relationshipSourceOrTarget instanceof TRequirement) {
            nodeTemplate = topologyTemplate.getNodeTemplates().stream()
                .filter(node -> Objects.nonNull(node.getRequirements()))
                .filter(node -> node.getRequirements().stream()
                    .anyMatch(requirement -> requirement.getId().equals(relationshipSourceOrTarget.getId()))
                ).findFirst();
        }

        return nodeTemplate.orElseThrow(NullPointerException::new);
    }

    public static void collectIdsOfExistingTopologyElements(TTopologyTemplate topologyTemplateB, Map<String, String> idMapping) {
        // collect existing node & relationship template ids
        topologyTemplateB.getNodeTemplateOrRelationshipTemplate()
            // the existing ids are left unchanged
            .forEach(x -> idMapping.put(x.getId(), x.getId()));

        // collect existing requirement ids
        topologyTemplateB.getNodeTemplates().stream()
            .filter(nt -> nt.getRequirements() != null)
            .forEach(nt -> nt.getRequirements()
                // the existing ids are left unchanged
                .forEach(x -> idMapping.put(x.getId(), x.getId()))
            );

        //collect existing capability ids
        topologyTemplateB.getNodeTemplates().stream()
            .filter(nt -> nt.getCapabilities() != null)
            .forEach(nt -> nt.getCapabilities()
                // the existing ids are left unchanged
                .forEach(x -> idMapping.put(x.getId(), x.getId()))
            );
    }

    public static void generateNewIdOfTemplate(HasId element, TTopologyTemplate topologyTemplate) {
        HashMap<String, String> map = new HashMap<>();
        collectIdsOfExistingTopologyElements(topologyTemplate, map);
        generateNewIdOfTemplate(element, map);
    }

    public static void generateNewIdOfTemplate(HasId element, Map<String, String> idMapping) {
        String newId = element.getId();
        while (idMapping.containsKey(newId)) {
            newId = newId + "-new";
        }
        idMapping.put(element.getId(), newId);
        element.setId(newId);
    }

    public static TRelationshipTemplate createRelationshipTemplate(TNodeTemplate sourceNode, TNodeTemplate targetNode, QName type) {
        return createRelationshipTemplate(sourceNode, targetNode, type, "");
    }

    public static TRelationshipTemplate createRelationshipTemplate(TNodeTemplate sourceNode, TNodeTemplate
        targetNode, QName type, String connectionDescription) {
        return new TRelationshipTemplate.Builder(
            "con-" + sourceNode.getId() + "-" + connectionDescription + "-" + targetNode.getId(),
            type,
            sourceNode,
            targetNode
        )
            .setName(type.getLocalPart())
            .build();
    }

    public static TRelationshipTemplate createRelationshipTemplateAndAddToTopology(TNodeTemplate sourceNode,
                                                                                   TNodeTemplate targetNode, QName type,
                                                                                   TTopologyTemplate topology) {
        return createRelationshipTemplateAndAddToTopology(sourceNode, targetNode, type, type.getLocalPart(), topology);
    }

    public static TRelationshipTemplate createRelationshipTemplateAndAddToTopology(TNodeTemplate sourceNode,
                                                                                   TNodeTemplate targetNode, QName type,
                                                                                   String connectionDescription, TTopologyTemplate topology) {
        TRelationshipTemplate relationshipTemplate = createRelationshipTemplate(sourceNode, targetNode, type, connectionDescription);
        generateNewIdOfTemplate(relationshipTemplate, topology);
        topology.addRelationshipTemplate(relationshipTemplate);
        return relationshipTemplate;
    }

    public static boolean nodeTypeHasInterface(TNodeType nodeType, String interfaceName) {
        return Objects.nonNull(nodeType.getInterfaces()) && nodeType.getInterfaces().stream()
            .anyMatch(nodeInterface -> interfaceName.equals(nodeInterface.getName()));
    }

    public static void addPolicy(TNodeTemplate node, QName policyType, String name) {
        List<TPolicy> policies = node.getPolicies();
        if (Objects.isNull(policies)) {
            policies = new ArrayList<>();
            node.setPolicies(policies);
        }

        policies.add(
            new TPolicy.Builder(policyType)
                .setName(name)
                .build()
        );
    }

    public static void addRequirement(TNodeTemplate node, QName requirementType, String name) {
        addRequirement(node, requirementType, name, name);
    }

    public static void addRequirement(TNodeTemplate node, QName requirementType, String name, String id) {
        List<TRequirement> requirements = node.getRequirements();
        if (Objects.isNull(requirements)) {
            requirements = new ArrayList<>();
            node.setRequirements(requirements);
        }

        requirements.add(
            new TRequirement.Builder(id, name, requirementType).build()
        );
    }

    public static void addCapability(TNodeTemplate node, QName capabilityType, String name) {
        addCapability(node, capabilityType, name, name);
    }

    public static void addCapability(TNodeTemplate node, QName capabilityType, String name, String id) {
        List<TCapability> capabilities = node.getCapabilities();
        if (Objects.isNull(capabilities)) {
            capabilities = new ArrayList<>();
            node.setCapabilities(capabilities);
        }

        capabilities.add(
            new TCapability.Builder(id, capabilityType, name).build()
        );
    }

    public static boolean containsPolicyType(TNodeTemplate node, QName policyType) {
        return Objects.nonNull(node.getPolicies()) &&
            node.getPolicies().stream()
                .anyMatch(policy -> policy.getPolicyType()
                    .equals(policyType)
                );
    }

    public static List<DeploymentTechnologyDescriptor> extractDeploymentTechnologiesFromServiceTemplate(
        TServiceTemplate serviceTemplate, ObjectMapper objectMapper) {
        return Optional.ofNullable(serviceTemplate.getTags())
            .map(tags -> extractDeploymentTechnologiesFromTags(tags, objectMapper))
            .orElseGet(ArrayList::new);
    }

    public static List<DeploymentTechnologyDescriptor> extractDeploymentTechnologiesFromTags(
        List<TTag> tags, ObjectMapper objectMapper) {
        return Optional.ofNullable(tags)
            .flatMap(tTags -> tTags.stream()
                .filter(tTag -> Objects.equals(tTag.getName(), TAG_DEPLOYMENT_TECHNOLOGIES))
                .findAny())
            .map(TTag::getValue)
            .map(s -> {
                CollectionType collectionType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, DeploymentTechnologyDescriptor.class);
                try {
                    return objectMapper.<List<DeploymentTechnologyDescriptor>>readValue(s, collectionType);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Deployment technologies tag could not be parsed as JSON", e);
                }
            })
            .orElseGet(ArrayList::new);
    }

    /**
     * Merge properties definitions. Only winery properties definitions are considered. The first element in the list is
     * the lowest in the inheritance hierarchy.
     */
    public static <T extends TEntityType> List<PropertyDefinitionKV> mergePropertiesDefinitions(List<T> entityTypes) {
        List<PropertyDefinitionKV> propertyDefinitions = new ArrayList<>();

        for (int i = 0; i < entityTypes.size(); i++) {
            TEntityType entityType = entityTypes.get(i);
            WinerysPropertiesDefinition winerysPropertiesDefinition = entityType.getWinerysPropertiesDefinition();

            // Continue if current entity type does not have any properties definitions
            if (winerysPropertiesDefinition == null) {
                continue;
            }

            // Continue if current entity type does not have any properties definitions
            List<PropertyDefinitionKV> winerysPropertiesDefinitions = winerysPropertiesDefinition.getPropertyDefinitions();
            if (winerysPropertiesDefinitions == null) {
                continue;
            }

            // Add property definition to list if not already added by a previous entity type
            for (PropertyDefinitionKV entityTypePropertyDefinition : winerysPropertiesDefinitions) {
                boolean exists = false;
                for (PropertyDefinitionKV propertyDefinition : propertyDefinitions) {
                    if (Objects.equals(propertyDefinition.getKey(), entityTypePropertyDefinition.getKey())) {
                        if (i == 1) {
                            propertyDefinition.setDerivedFromStatus("OVERRIDE");
                        }
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    entityTypePropertyDefinition.setDerivedFromType(entityType.getQName());

                    if (i == 0) {
                        entityTypePropertyDefinition.setDerivedFromStatus("SELF");
                    } else {
                        entityTypePropertyDefinition.setDerivedFromStatus("INHERITED");
                    }

                    propertyDefinitions.add(entityTypePropertyDefinition);
                }
            }
        }

        return propertyDefinitions;
    }

    public static <T extends TEntityType> WinerysPropertiesDefinition getEffectiveWineryPropertyDefinitions(List<T> hierarchy) {
        List<PropertyDefinitionKV> propertyDefinitions = ModelUtilities.mergePropertiesDefinitions(hierarchy);

        // Convention defines that the first element in the list is the child
        T child = hierarchy.get(0);

        // Create new WPD
        WinerysPropertiesDefinition winerysPropertiesDefinition = new WinerysPropertiesDefinition();
        winerysPropertiesDefinition.setElementName(child.getName());
        winerysPropertiesDefinition.setNamespace(child.getTargetNamespace());
        winerysPropertiesDefinition.setPropertyDefinitions(propertyDefinitions);

        return winerysPropertiesDefinition;
    }

    /**
     * Check if two lists are the same. Order does not matter. Null is handled as empty list.
     */
    public static <T> boolean compareUnorderedNullableLists(List<T> first, List<T> second) {
        if (first == null) first = new ArrayList<T>();
        if (second == null) second = new ArrayList<T>();

        return first.containsAll(second) && first.size() == second.size();
    }
}
