import { BehaviorSubject } from 'rxjs/BehaviorSubject';

/*******************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
export interface Software {
    id: string;
    name: string;
    url: string;
    branch: string;
    status: Status;
    files: number;
    licensesEffective: Array<string>;
    filesExcluded: number;
    licensesAll: Array<string>;
}

export class LicenseTree {
    name: string;
    files?: LicenseTree[];
    ismarktodelte: boolean;
}

export class LicenseTreeFlatNode {
    expandable: boolean;
    name: string;
    level: number;
}

export enum Status {
    QUEUED = 'QUEUED',
    UPLOADING = 'UPLOADING',
    ANALYZING = 'ANALYZING',
    FINISHED = 'FINISHED',
    FAILED = 'FAILED',
}

export interface License {
    id: string;
    name: string;
    notes: string;
    furtherInformation: Array<string>;
}
