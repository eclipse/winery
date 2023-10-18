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

package org.eclipse.winery.model.jaxbsupport.map;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.winery.model.converter.support.Namespaces;
import org.eclipse.winery.model.tosca.TEntityTemplate;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * <p>
 * This XmlAdapter is used to transform between the different {@link TEntityTemplate.Properties} implementations and
 * a "raw" xml {@link Element}. Because {@link Element} is an interface, the XmlAdapter declares <tt>Object</tt> as it's
 * "intermediate" type.
 * <p>
 * This use has the additional benefit of removing the explicit LinkedHashMap declarations in both {@link org.eclipse.winery.model.tosca.TEntityTemplate.WineryKVProperties}
 * and {@link org.eclipse.winery.model.tosca.TEntityTemplate.YamlProperties} from the JAXBContext, thus avoiding a
 * pollution with the namespace URI "" by the use of LinkedHashMap.
 */
public class PropertiesAdapter extends XmlAdapter<PropertiesAdapter.Union, TEntityTemplate.Properties> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesAdapter.class);

    private enum PropertyKind {
        XML, YAML, KV
    }

    @Nullable
    private final NamespacePrefixMapper prefixMapper;

    public PropertiesAdapter() {
        this.prefixMapper = null;
    }

    public PropertiesAdapter(@Nullable NamespacePrefixMapper prefixMapper) {
        this.prefixMapper = prefixMapper;
    }

    @Override
    public TEntityTemplate.Properties unmarshal(Union xmlData) {
        if (xmlData == null) {
            return null;
        }
        if (!(xmlData.any instanceof Element)) {
            throw new IllegalStateException("Cannot deserialize arbitrary XML if no Element is available. Expected Element, got " + xmlData.getClass().getName());
        }
        Element element = (Element)xmlData.any;
        final PropertyKind kind = determinePropertyKind(element);
        switch (kind) {
            case XML:
                TEntityTemplate.XmlProperties xmlProps = new TEntityTemplate.XmlProperties();
                xmlProps.setAny(element);
                return xmlProps;
            case YAML:
                TEntityTemplate.YamlProperties yamlProperties = new TEntityTemplate.YamlProperties();
                // deserialize into hierarchical map
                LinkedHashMap<String, Object> hierarchy = unmarshallNestedMap(element);
                yamlProperties.setProperties(hierarchy);
                return yamlProperties;
            case KV:
                final String namespace = element.getNamespaceURI();
                final String elementName = element.getLocalName();

                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                NodeList childNodes = element.getChildNodes();
                for (int x = 0, size = childNodes.getLength(); x < size; x++) {
                    Node childNode = childNodes.item(x);
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        map.put(childNode.getLocalName(), childNode.getTextContent());
                    }
                }
                TEntityTemplate.WineryKVProperties result = new TEntityTemplate.WineryKVProperties();
                result.setNamespace(namespace);
                result.setElementName(elementName);
                result.setKVProperties(map);
                return result;
        }
        throw new IllegalStateException("Could not determine the correct property kind for unmarshalling");
    }

    private LinkedHashMap<String, Object> unmarshallNestedMap(Element element) {
        LinkedHashMap<String, Object> results = new LinkedHashMap<>();
        NodeList children = element.getChildNodes();
        for (int x = 0, size = children.getLength(); x < size; x++) {
            Node child = children.item(x);
            if (child.getChildNodes().getLength() > 1) {
                results.put(child.getLocalName(), unmarshallNestedMap((Element)child));
            } else if (child.getChildNodes().getLength() == 1) {
                results.put(child.getLocalName(), child.getTextContent());
            } else {
                results.put(child.getLocalName(), "");
            }
        }
        return results;
    }

    private PropertyKind determinePropertyKind(Element element) {
        if (element.getNamespaceURI().equals(Namespaces.TOSCA_YAML_NS)) {
            return PropertyKind.YAML;
        }
        return isKeyValuePropertyDefinition(element) ? PropertyKind.KV : PropertyKind.XML;
    }

    @Override
    public Union marshal(TEntityTemplate.Properties jaxb) {
        if (jaxb == null) {
            return null;
        }
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
        dbf.setNamespaceAware(true);
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Could not instantiate document builder", e);
        }
        Document doc = db.newDocument();
        // Because JAXB is unwrapping the top-level element and discarding it, we add this extra wrapper
        Element serializationWrapper = doc.createElement("DISCARDED");
        doc.appendChild(serializationWrapper);

        Union result = new Union();
        if (jaxb instanceof TEntityTemplate.WineryKVProperties) {
            result.setAny(marshallWineryKV((TEntityTemplate.WineryKVProperties) jaxb, doc));
        } else if (jaxb instanceof TEntityTemplate.XmlProperties) {
            // assume XmlProperties are correctly stored as xml
            Object any = ((TEntityTemplate.XmlProperties) jaxb).getAny();
            if (!(any instanceof Element)) {
                LOGGER.error("XmlProperties did not contain an Xml Element as any. Aborting serialization");
                return null;
            }
            result.setAny(any);
        } else if (jaxb instanceof TEntityTemplate.YamlProperties) {
            LinkedHashMap<String, Object> data = ((TEntityTemplate.YamlProperties) jaxb).getProperties();
            result.setAny(marshallNestedMap(data, doc));
        } else {
            throw new IllegalStateException("Encountered Unknown Property Subclass during Serialization");
        }
        return result;
    }

    private Element marshallNestedMap(LinkedHashMap<String, Object> data, Document doc) {
        final String prefix = (prefixMapper != null)
            ? prefixMapper.getPreferredPrefix(Namespaces.TOSCA_YAML_NS, "", true)
            : "";
        Element root = doc.createElementNS(Namespaces.TOSCA_YAML_NS, "Properties");
        root.setPrefix(prefix);
        for (String key : data.keySet()) {
            final Object value = data.get(key);
            if (value instanceof Map) {
                root.appendChild(marshallNestedMap(doc, key, (Map<String, Object>)value));
            } else if (value instanceof String) {
                Element entry = doc.createElement(key);
                entry.appendChild(doc.createTextNode((String) value));
            } else {
                LOGGER.warn("Could not serialize value of type {}. Skipping!", value.getClass().getName());
            }
        }
        return root;
    }

    private Element marshallNestedMap(Document doc, String elementName, Map<String, Object> data) {
        Element container = doc.createElement(elementName);
        for (String key : data.keySet()) {
            final Object value = data.get(key);
            if (value instanceof Map) {
                container.appendChild(marshallNestedMap(doc, key, (Map<String, Object>)value));
            } else if (value instanceof String) {
                Element entry = doc.createElement(key);
                entry.appendChild(doc.createTextNode((String) value));
            } else {
                LOGGER.warn("Could not serialize value of type {}. Skipping!", value.getClass().getName());
            }
        }
        return container;
    }

    private Element marshallWineryKV(TEntityTemplate.WineryKVProperties jaxb, Document doc) {
        String namespace = jaxb.getNamespace();
        String elementName = jaxb.getElementName();
        if (namespace == null) {
            namespace = org.eclipse.winery.model.tosca.constants.Namespaces.EXAMPLE_NAMESPACE_URI;
        }
        if (elementName == null || elementName.equals("")) {
            elementName = "Properties";
        }

        final String prefix = (prefixMapper != null)
            ? prefixMapper.getPreferredPrefix(namespace, "", true)
            : "";
        Element root = doc.createElementNS(namespace, elementName);
        root.setPrefix(prefix);

        // No wpd - so this is not possible:
        // we produce the serialization in the same order the XSD would be generated (because of the usage of xsd:sequence)
        // for (PropertyDefinitionKV prop : wpd.getPropertyDefinitionKVList()) {
        final LinkedHashMap<String, String> properties = jaxb.getKVProperties();
        for (String key : properties.keySet()) {
            Element element = doc.createElementNS(namespace, key);
            element.setPrefix(prefix);
            root.appendChild(element);
            String value = properties.get(key);
            if (value != null) {
                Text text = doc.createTextNode(value);
                element.appendChild(text);
            }
        }
        return root;
    }

    public static boolean isKeyValuePropertyDefinition(Element xmlElement) {
        final NodeList xmlChildren = xmlElement.getChildNodes();
        if (xmlChildren.getLength() == 0) {
            return false;
        }
        // if any of the child nodes has multiple children it's not KVProps
        for (int i = 0; i < xmlChildren.getLength(); i++) {
            Node item = xmlChildren.item(i);
            if (item instanceof Text || item instanceof Comment) {
                continue;
            }
            if (!(item instanceof Element)) {
                // this is definitively not a KVProp schema conform xml tree
                return false;
            }
            if (item.getChildNodes().getLength() > 1) {
                // more than a single child element at a key, so not KVProp
                return false;
            }
        }
        return true;
    }

    @XmlRootElement(name = "Properties", namespace = org.eclipse.winery.model.tosca.constants.Namespaces.TOSCA_NAMESPACE)
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "Properties", namespace = org.eclipse.winery.model.tosca.constants.Namespaces.TOSCA_NAMESPACE)
    public static class Union {
        @XmlAnyElement(lax = true)
        private Object any;

        public Object getAny() {
            return any;
        }

        public void setAny(Object any) {
            this.any = any;
        }
    }
}
