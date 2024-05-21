/********************************************************************************
 * Copyright (c) 2017-2021 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.yaml.visitor;

import org.eclipse.winery.model.tosca.yaml.YTArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.YTArtifactType;
import org.eclipse.winery.model.tosca.yaml.YTAttributeAssignment;
import org.eclipse.winery.model.tosca.yaml.YTAttributeDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCallOperationActivityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityAssignment;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityDefinition;
import org.eclipse.winery.model.tosca.yaml.YTCapabilityType;
import org.eclipse.winery.model.tosca.yaml.YTConstraintClause;
import org.eclipse.winery.model.tosca.yaml.YTDataType;
import org.eclipse.winery.model.tosca.yaml.YTEntityType;
import org.eclipse.winery.model.tosca.yaml.YTEventFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTGroupDefinition;
import org.eclipse.winery.model.tosca.yaml.YTGroupType;
import org.eclipse.winery.model.tosca.yaml.YTImplementation;
import org.eclipse.winery.model.tosca.yaml.YTImportDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceAssignment;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.YTInterfaceType;
import org.eclipse.winery.model.tosca.yaml.YTNodeFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTNodeTemplate;
import org.eclipse.winery.model.tosca.yaml.YTNodeType;
import org.eclipse.winery.model.tosca.yaml.YTOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.YTParameterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPolicyType;
import org.eclipse.winery.model.tosca.yaml.YTPropertyAssignment;
import org.eclipse.winery.model.tosca.yaml.YTPropertyDefinition;
import org.eclipse.winery.model.tosca.yaml.YTPropertyFilterDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipAssignment;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipTemplate;
import org.eclipse.winery.model.tosca.yaml.YTRelationshipType;
import org.eclipse.winery.model.tosca.yaml.YTRepositoryDefinition;
import org.eclipse.winery.model.tosca.yaml.YTRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.YTRequirementDefinition;
import org.eclipse.winery.model.tosca.yaml.YTSchemaDefinition;
import org.eclipse.winery.model.tosca.yaml.YTServiceTemplate;
import org.eclipse.winery.model.tosca.yaml.YTSubstitutionMappings;
import org.eclipse.winery.model.tosca.yaml.YTTopologyTemplateDefinition;
import org.eclipse.winery.model.tosca.yaml.YTTriggerDefinition;
import org.eclipse.winery.model.tosca.yaml.YTVersion;
import org.eclipse.winery.model.tosca.yaml.YTWorkflow;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;

public interface IVisitor<R extends AbstractResult, P extends AbstractParameter> {
    R visit(YTArtifactDefinition node, P parameter);

    R visit(YTArtifactType node, P parameter);

    R visit(YTAttributeAssignment node, P parameter);

    R visit(YTAttributeDefinition node, P parameter);

    R visit(YTCallOperationActivityDefinition node, P parameter);

    R visit(YTCapabilityAssignment node, P parameter);

    R visit(YTCapabilityDefinition node, P parameter);

    R visit(YTCapabilityType node, P parameter);

    R visit(YTConstraintClause node, P parameter);

    R visit(YTDataType node, P parameter);

    R visit(YTEntityType node, P parameter);

    R visit(YTEventFilterDefinition node, P parameter);

    R visit(YTSchemaDefinition node, P parameter);

    R visit(YTGroupDefinition node, P parameter);

    R visit(YTGroupType node, P parameter);

    R visit(YTImplementation node, P parameter);

    R visit(YTImportDefinition node, P parameter);

    R visit(YTInterfaceAssignment node, P parameter);

    R visit(YTInterfaceDefinition node, P parameter);

    R visit(YTInterfaceType node, P parameter);

    R visit(YTNodeFilterDefinition node, P parameter);

    R visit(YTNodeTemplate node, P parameter);

    R visit(YTNodeType node, P parameter);

    R visit(YTOperationDefinition node, P parameter);

    R visit(YTParameterDefinition node, P parameter);

    R visit(YTPolicyDefinition node, P parameter);

    R visit(YTPolicyType node, P parameter);

    R visit(YTPropertyAssignment node, P parameter);

    R visit(YTPropertyDefinition node, P parameter);

    R visit(YTPropertyFilterDefinition node, P parameter);

    R visit(YTRelationshipAssignment node, P parameter);

    R visit(YTRelationshipDefinition node, P parameter);

    R visit(YTRelationshipTemplate node, P parameter);

    R visit(YTRelationshipType node, P parameter);

    R visit(YTRepositoryDefinition node, P parameter);

    R visit(YTRequirementAssignment node, P parameter);

    R visit(YTRequirementDefinition node, P parameter);

    R visit(YTServiceTemplate node, P parameter);

    R visit(YTSubstitutionMappings node, P parameter);

    R visit(YTTriggerDefinition node, P parameter);

    R visit(YTTopologyTemplateDefinition node, P parameter);

    R visit(YTVersion node, P parameter);

    R visit(YTWorkflow node, P parameter);

    R visit(Metadata node, P parameter);
}
