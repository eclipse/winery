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

package org.eclipse.winery.edmm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.edmm.model.EdmmType;
import org.eclipse.winery.edmm.toscalight.ToscaLightChecker;
import org.eclipse.winery.model.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EdmmUtils {

    public static final String IMPORTED_EDMM_NAMESPACE = "https://opentosca.org/edmm/imported/";
    private static final Logger LOGGER = LoggerFactory.getLogger(EdmmUtils.class);

    public static Map<String, Object> checkToscaLightCompatibility(TServiceTemplate serviceTemplate) {
        ToscaLightChecker toscaLightChecker = getToscaLightChecker();

        boolean toscaLightCompliant = toscaLightChecker.isToscaLightCompliant(serviceTemplate);
        Map<QName, List<String>> errorList = toscaLightChecker.getErrorList();
        errorList.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        HashMap<String, Object> map = new HashMap<>();
        map.put("isToscaLightCompatible", toscaLightCompliant);
        map.put("errorList", errorList);

        return map;
    }

    public static ToscaLightChecker getToscaLightChecker() {
        IRepository repository = RepositoryFactory.getRepository();

        Map<QName, TRelationshipType> relationshipTypes = repository.getQNameToElementMapping(RelationshipTypeId.class);
        Map<QName, TNodeType> nodeTypes = repository.getQNameToElementMapping(NodeTypeId.class);
        Map<QName, EdmmType> oneToOneMap = EdmmManager.forRepository(repository).getToscaToEdmmMap();

        return new ToscaLightChecker(nodeTypes, relationshipTypes, oneToOneMap);
    }

    public static Map<QName, TServiceTemplate> getAllToscaLightCompliantModels() {
        Map<QName, TServiceTemplate> serviceTemplates = RepositoryFactory.getRepository()
            .getQNameToElementMapping(ServiceTemplateId.class);

        ToscaLightChecker toscaLightChecker = EdmmUtils.getToscaLightChecker();

        return serviceTemplates.entrySet()
            .stream()
            .filter(entry -> toscaLightChecker.isToscaLightCompliant(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static String normalizeQName(QName qName) {
        return qName.toString()
            .replace("{", "")
            .replace("}", "__")
            .replace("/", "--")
            .replace(':', '+');
    }

    /**
     * Tries to generate a QName from a type name. Thereby, two underscores are treated as the separation between a
     * potential namespace and the ID
     *
     * @param typeName the name of the EDMM type
     * @return a QName
     */
    public static QName getQNameFromType(String typeName, String namespaceSuffix) {
        if (typeName.contains("__")) {
            return QName.valueOf(
                "{" +
                    typeName.replace('+', ':')
                        .replace("__", "}")
                        .replace("--", "/")
            );
        }

        return new QName(
            IMPORTED_EDMM_NAMESPACE + namespaceSuffix,
            typeName
        );
    }
}
