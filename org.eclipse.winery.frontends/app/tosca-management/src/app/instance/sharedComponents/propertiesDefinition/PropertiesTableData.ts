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
export class PropertiesTableData {
    key: string = null;
    type: string = null;
    required: boolean;
    defaultValue: string;
    description: string;
    constraints: string;
    constructor(key: string, type: string, required: boolean, defaultValue: string, description: string, constraints: string) {
        this.key = key;
        this.type = type;
        this.required = required ? required : false;
        this.defaultValue = defaultValue;
        this.description = description;
        this.constraints = constraints;
    }
}
