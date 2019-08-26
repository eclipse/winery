/********************************************************************************
 * Copyright (c) 2017-2019 Contributors to the Eclipse Foundation
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

export class AbstractTTemplate {
    constructor(public documentation?: any,
                public any?: any,
                public otherAttributes?: any) {
    }
}

/**
 * This is the datamodel for node Templates and relationship templates
 */
export class TTopologyTemplate extends AbstractTTemplate {
    nodeTemplates: Array<TNodeTemplate> = [];
    relationshipTemplates: Array<TRelationshipTemplate> = [];
}

/**
 * This is the datamodel for node Templates
 */
export class TNodeTemplate extends AbstractTTemplate {

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
                public capabilities?: any,
                public requirements?: any,
                public deploymentArtifacts?: any,
                public policies?: any,
                private _state?: DifferenceStates) {
        super(documentation, any, otherAttributes);
    }

    /**
     * needed for the winery redux reducer,
     * updates a specific attribute and returns a whole new node template
     * @param indexOfUpdatedAttribute: index of the to be updated attribute in the constructor
     * @param updatedValue: the new value
     *
     * @return nodeTemplate: a new node template with the updated value
     */
    generateNewNodeTemplateWithUpdatedAttribute(updatedAttribute: string, updatedValue: any): TNodeTemplate {
        const nodeTemplate = new TNodeTemplate(this.properties, this.id, this.type, this.name, this.minInstances, this.maxInstances,
            this.visuals, this.documentation, this.any, this.otherAttributes, this.x, this.y, this.capabilities,
            this.requirements, this.deploymentArtifacts, this.policies);
        if (updatedAttribute === 'coordinates') {
            nodeTemplate.x = updatedValue.x;
            nodeTemplate.y = updatedValue.y;
        } else if (updatedAttribute === 'location') {
            let newOtherAttributesAssigned: boolean;
            let nameSpace: string;
            for (const key in nodeTemplate.otherAttributes) {
                if (nodeTemplate.otherAttributes.hasOwnProperty(key)) {
                    nameSpace = key.substring(key.indexOf('{'), key.indexOf('}') + 1);
                    if (nameSpace) {
                        const otherAttributes = {
                            [nameSpace + 'location']: updatedValue,
                            [nameSpace + 'x']: nodeTemplate.x,
                            [nameSpace + 'y']: nodeTemplate.y
                        };
                        nodeTemplate.otherAttributes = otherAttributes;
                        newOtherAttributesAssigned = true;
                        break;
                    }
                }
            }
            if (!newOtherAttributesAssigned) {
                const otherAttributes = {
                    'location': updatedValue,
                };
                nodeTemplate.otherAttributes = otherAttributes;
            }
            console.log(nodeTemplate);
        } else if (updatedAttribute === ('minInstances') || updatedAttribute === ('maxInstances')) {
            if (Number.isNaN(+updatedValue)) {
                nodeTemplate[updatedAttribute] = updatedValue;
            } else {
                nodeTemplate[updatedAttribute] = +updatedValue;
            }
        } else {
            console.log(updatedValue);
            nodeTemplate[updatedAttribute] = updatedValue;
            console.log(nodeTemplate);
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
 * This is the datamodel for the Entity Types
 */
export class EntityType extends Entity {
    constructor(id: string,
                qName: string,
                name: string,
                namespace: string,
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

/**
 * This is the datamodel for relationship templates
 */
export class TRelationshipTemplate extends AbstractTTemplate {

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
