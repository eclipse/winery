/*******************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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
import { YesNoEnum } from './enums';

// possible types for TOSCA in XML format, used for Parameters or Properties
export type XmlTypes = 'xsd:string' | 'xsd:float' | 'xsd:decimal' | 'xsd:anyURI' | 'xsd:QName';

// see https://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.3/os/TOSCA-Simple-Profile-YAML-v1.3-os.html#_Toc26969444
// these types are allowed to be used for Parameters and Properties if YAML mode is used
export type YamlTypes = 'string' | 'integer' | 'float' | 'boolean' | 'timestamp';

export class InterfaceParameter {

    name: string;
    type: string;
    required: YesNoEnum;

    constructor(name: string, type: string, required: YesNoEnum) {
        this.name = name;
        this.type = type;
        this.required = required;
    }
}

export class Parameter {
    key: string = null;
    type = 'string';
    description = '';
    required = false;
    defaultValue = '';
    value = '';
}
