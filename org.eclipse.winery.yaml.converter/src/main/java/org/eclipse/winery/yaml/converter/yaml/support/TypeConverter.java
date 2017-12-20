/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.yaml.converter.yaml.support;

import javax.xml.namespace.QName;

import org.eclipse.winery.yaml.common.Namespaces;

public class TypeConverter {
    public static TypeConverter INSTANCE = new TypeConverter();

    public QName convert(QName type) {
        if (type.getNamespaceURI().equals(Namespaces.YAML_NS)) {
            switch (type.getLocalPart()) {
                case "string":
                    return new QName(Namespaces.XML_NS, "string", "xsd");
                case "integer":
                case "float":
                    return new QName(Namespaces.XML_NS, "decimal", "xsd");
                case "boolean":
                    return new QName(Namespaces.XML_NS, "boolean", "xsd");
                case "timestamp":
                    return new QName(Namespaces.XML_NS, "date", "xsd");
                case "null":
                    return new QName(Namespaces.XML_NS, "string", "xsd");
                default:
                    return type;
            }
        }
        return type;
    }

    public Class convertToJavaType(QName type) {
        if (type.getNamespaceURI().equals(Namespaces.YAML_NS)) {
            switch (type.getLocalPart()) {
                case "integer":
                    return Integer.class;
                case "float":
                    return Float.class;
                case "boolean":
                    return Boolean.class;
                case "timestamp":
                case "null":
                case "string":
                default:
                    return String.class;
            }
        } else {
            return Object.class;
        }
    }
}
