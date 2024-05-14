/********************************************************************************
 * Copyright (c) 2017-2022 Contributors to the Eclipse Foundation
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
 ********************************************************************************/
import { DifferenceStates, VersionUtils } from './ToscaDiff';
import { Visuals } from './visuals';
import { TPolicy } from './policiesModalData';
import { Interface } from '../../../../tosca-management/src/app/model/interfaces';
import { PropertiesDefinition } from '../../../../tosca-management/src/app/instance/sharedComponents/propertiesDefinition/propertiesDefinition.types';
import { Constraint } from '../../../../tosca-management/src/app/model/constraint';
import { NodeTemplateInstanceStates } from './enums';
import { CapabilityModel } from './capabilityModel';
import { RequirementModel } from './requirementModel';
import { TOSCA_WINERY_EXTENSIONS_NAMESPACE } from './namespaces';

export class AbstractTEntity {
    constructor(public documentation?: any,
                public any?: any,
                public otherAttributes?: any) {
    }
}

export class TGroupDefinition extends AbstractTEntity {

    constructor(public name: string,
                public description: string,
                public members: string[],
                public properties?: any,
                documentation?: any,
                any?: any,
                otherAttributes?: any) {
        super(documentation, any, otherAttributes);
    }
}

export interface OTParticipant {
    name: string;
    url: string;
}

/**
 * This is the data model for node Templates and relationship templates
 */
export class TTopologyTemplate extends AbstractTEntity {
    nodeTemplates: Array<TNodeTemplate> = [];
    relationshipTemplates: Array<TRelationshipTemplate> = [];
    policies: Array<TPolicy>;
    groups: Array<TGroupDefinition> = [];
    participants: Array<OTParticipant> = [];
}

export class TArtifact extends AbstractTEntity {
    constructor(public id: string,
                public type: string,
                public file: string,
                public targetLocation?: string,
                public properties?: any,
                public documentation?: any,
                public any?: any,
                public otherAttributes?: any) {
        super(documentation, any, otherAttributes);
    }
}

/**
 * This is the data-model for node Templates
 */
export class TNodeTemplate extends AbstractTEntity {

    constructor(public properties: any,
                public id: string,
                public type: string,
                public name: string,
                public minInstances: number,
                public maxInstances: number,
                public visuals: Visuals,
                documentation?: any,
                any?: any,
                otherAttributes?: any,
                public x?: number,
                public y?: number,
                public capabilities?: CapabilityModel[],
                public requirements?: RequirementModel[],
                public deploymentArtifacts?: any[],
                public policies?: Array<TPolicy>,
                public artifacts?: Array<TArtifact>,
                public instanceState?: NodeTemplateInstanceStates,
                public valid?: boolean,
                public working?: boolean,
                private _state?: DifferenceStates) {
        super(documentation, any, otherAttributes);
    }

    /**
     * needed for the winery redux reducer,
     * updates a specific attribute and returns a whole new node template
     * @param updatedAttribute: index of the to be updated attribute in the constructor
     * @param updatedValue: the new value
     *
     * @return nodeTemplate: a new node template with the updated value
     */
    generateNewNodeTemplateWithUpdatedAttribute(updatedAttribute: string, updatedValue: any): TNodeTemplate {
        const nodeTemplate = new TNodeTemplate(this.properties, this.id, this.type, this.name, this.minInstances, this.maxInstances,
            this.visuals, this.documentation, this.any, this.otherAttributes, this.x, this.y, this.capabilities,
            this.requirements, this.deploymentArtifacts, this.policies, this.artifacts, this.instanceState, this.valid, this.working, this._state);

        const namespace = '{' + TOSCA_WINERY_EXTENSIONS_NAMESPACE + '}';
        if (updatedAttribute === 'coordinates') {
            nodeTemplate.x = updatedValue.x;
            nodeTemplate.y = updatedValue.y;
            nodeTemplate.otherAttributes[namespace + 'x'] = updatedValue.x;
            nodeTemplate.otherAttributes[namespace + 'y'] = updatedValue.y;

        } else if (updatedAttribute === 'location' || updatedAttribute === 'participant' || updatedAttribute === 'deployment-technology') {
            if (updatedValue.length === 0) {
                delete nodeTemplate.otherAttributes[namespace + updatedAttribute];
            } else {
                nodeTemplate.otherAttributes[namespace + updatedAttribute] = updatedValue;
            }

        } else if (updatedAttribute === ('minInstances') || updatedAttribute === ('maxInstances')) {
            if (Number.isNaN(+updatedValue)) {
                nodeTemplate[updatedAttribute] = updatedValue;
            } else {
                nodeTemplate[updatedAttribute] = +updatedValue;
            }
        } else {
            nodeTemplate[updatedAttribute] = updatedValue;
        }
        return nodeTemplate;
    }

    public get state(): DifferenceStates {
        return this._state;
    }

    public set state(value: DifferenceStates) {
        this._state = value;
        this.visuals.color = VersionUtils.getElementColorByDiffState(value);
    }

    public deleteAppendix() {
        delete this._state;
        delete this.instanceState;
        delete this.valid;
        delete this.working;
        delete this.visuals;
    }
}

export interface Full<T> {
    serviceTemplateOrNodeTypeOrNodeTypeImplementation: T[];
}

export class Entity {
    constructor(public id: string,
                public qName: string,
                public name: string,
                public namespace: string,
                public properties?: any) {
    }
}

/**
 * This is the data-model for the Entity Types
 */
export class EntityType extends Entity {
    constructor(id: string = '',
                qName: string = '',
                name: string = '',
                namespace: string = '',
                properties?: any,
                public full?: any) {
        super(id, qName, name, namespace, properties);
    }
}

export class VisualEntityType extends EntityType {
    constructor(id: string,
                qName: string,
                name: string,
                namespace: string,
                properties: any,
                public color: string,
                public full: any,
                public visuals?: Visuals) {
        super(id, qName, name, namespace, properties, full);
    }
}

export class TPolicyType extends EntityType {
    constructor(id: string,
                qName: string,
                name: string,
                namespace: string,
                properties: any,
                public full: any,
                public targets?: string[]) {
        super(id, qName, name, namespace, properties, full);
    }
}

export class TDataType extends EntityType {
    constructor(id: string,
                qName: string,
                name: string,
                namespace: string,
                properties: any,
                public full: any,
                public constraints: Constraint[] = [],
                public keySchema: SchemaDefinition = undefined,
                public entrySchema: SchemaDefinition = undefined) {
        super(id, qName, name, namespace, properties, full);
    }
}

export class SchemaDefinition {
    constructor(public type: string,
                public description: string = '',
                public constraints: Constraint[] = [],
                public keySchema: SchemaDefinition = undefined,
                public entrySchema: SchemaDefinition = undefined) {
    }
}

export class TArtifactType extends EntityType {
    constructor(id: string,
                qName: string,
                name: string,
                namespace: string,
                full?: any,
                properties?: any,
                public mimeType?: string,
                public fileExtensions?: string[]) {
        super(id, qName, name, namespace, properties, full);
    }
}

/**
 * This is the data-model for relationship templates
 */
export class TRelationshipTemplate extends AbstractTEntity {

    constructor(public sourceElement: { ref: string },
                public targetElement: { ref: string },
                public name?: string,
                public id?: string,
                public type?: string,
                public properties?: any,
                documentation?: any,
                any?: any,
                otherAttributes?: any,
                public state?: DifferenceStates,
                public policies?: any) {
        super(documentation, any, otherAttributes);
    }

    /**
     * needed for the winery redux reducer,
     * updates a specific attribute and returns the whole new relationship template
     * @param updatedAttribute: index of the to be updated attribute in the constructor
     * @param updatedValue: the new value
     *
     * @return relTemplate: a new relationship template with the updated value
     */
    generateNewRelTemplateWithUpdatedAttribute(updatedAttribute: string, updatedValue: any): TRelationshipTemplate {
        const relTemplate = new TRelationshipTemplate(this.sourceElement, this.targetElement, this.name, this.id, this.type, this.properties,
            this.documentation, this.any, this.otherAttributes, this.state, this.policies);
        relTemplate[updatedAttribute] = updatedValue;
        return relTemplate;
    }

}

export class TNodeType extends AbstractTEntity {
    constructor(public name: string,
                public interfaces: { interfaces: Interface[] },
                public propertiesDefinition: PropertiesDefinition,
                public derivedFrom: any,
                documentation?: any,
                any?: any,
                other?: any) {
        super(documentation, any, other);
    }
}
