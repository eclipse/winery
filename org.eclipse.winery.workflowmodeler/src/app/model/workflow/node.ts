/**
 * Copyright (c) 2017 ZTE Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v20.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     ZTE - initial API and implementation and/or initial documentation
 */
import { Parameter } from './parameter';
import { Position } from './position';
import { Template } from './Template';

export class Node {
    public connection = [];
    public id: string;
    public input = new Array<Parameter>();
    public name: string;
    public nodeInterface: string;
    public nodeOperation: string;
    public nodeTemplate: string;
    public output = new Array<Parameter>();
    public position = new Position();
    public template = new Template();
    public type: string;

}
