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

import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { LicenseTree, LicenseTreeFlatNode } from './LicenseEngineApiData';
import { Injectable } from '@angular/core';
import { LicenseEngineService } from './licenseEngine.service';
import { InstanceService } from '../instance/instance.service';

@Injectable()
export class ChecklistDatabase {
    dataChange = new BehaviorSubject<LicenseTree[]>([]);
    TREE_DATA = [];
    parentNodeMap = new Map<LicenseTree, LicenseTree>();

    get data(): LicenseTree[] {
        return this.dataChange.value;
    }

    constructor(private leService: LicenseEngineService, private sharedData: InstanceService
    ) {
        this.initialize();
    }

    initialize() {
        // Build the tree nodes from Json object. The result is a list of `TodoItemNode` with nested
        //     file node as children.
        const data = this.buildFileTree(this.TREE_DATA, 0);

        // Notify the change.
        this.dataChange.next(data);
    }

    /**
     * Build the file structure tree. The `value` is the Json object, or a sub-tree of a Json object.
     * The return value is the list of `TodoItemNode`.
     */
    buildFileTree(obj: { [key: string]: any }, level: number): LicenseTree[] {
        return Object.keys(obj).reduce<LicenseTree[]>((accumulator, key) => {
            const value = obj[key];
            const node = new LicenseTree();
            node.name = key;

            if (value != null) {
                if (typeof value === 'object') {
                    node.files = this.buildFileTree(value, level + 1);
                } else {
                    node.name = value;
                }
            }

            return accumulator.concat(node);
        }, []);
    }

    insertItem(parent: LicenseTree, name: string) {
        if (!parent.files) {

            parent.files = [];
        }
        parent.files.push({ name } as LicenseTree);
        this.dataChange.next(this.data);
    }

    public deleteItem(parent: LicenseTree, name: string): void {
        if (parent.files) {
            parent.files = parent.files.filter((c) => c.name !== name);
            this.dataChange.next(this.data);
        }
        const toscaElements = this.sharedData.path.split('/');
        const toscaElementID = toscaElements[toscaElements.length - 1];
        const lst: String[] = [name];
        this.leService.excludeFile(lst).subscribe((success) => {
        }, () => {
        });
    }

    public findParent(id: number, node: any): any {

        if (node && node.id === id) {
            return node;
        } else {
            for (const element in node.children) {
                if (node.children[element].children && node.children[element].children.length > 0) {
                    return this.findParent(id, node.children[element]);
                }
            }
        }
    }
    updateItem(node: LicenseTree, name: string) {
        node.name = name;
        this.dataChange.next(this.data);
    }
}
