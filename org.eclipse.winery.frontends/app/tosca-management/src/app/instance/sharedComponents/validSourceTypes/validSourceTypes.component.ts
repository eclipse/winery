/*******************************************************************************
 * Copyright (c) 2019-2020 Contributors to the Eclipse Foundation
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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { ValidSourceTypesService } from './validSourceTypes.service';
import { WineryTableColumn } from '../../../wineryTableModule/wineryTable.component';
import { InstanceService } from '../../instance.service';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { SelectData } from '../../../model/selectData';
import { ValidSourceTypesApiData } from './validSourceTypesApiData';
import { HttpErrorResponse } from '@angular/common/http';
import { BsModalRef, BsModalService, ModalDirective } from 'ngx-bootstrap';
import { forkJoin } from 'rxjs';
import { QNameApiData } from '../../../model/qNameApiData';
import { QName } from '../../../../../../shared/src/app/model/qName';
import { ToscaTypes } from '../../../model/enums';

@Component({
    selector: 'winery-nodetype-selector',
    templateUrl: 'validSourceTypes.component.html',
    styleUrls: [
        'validSourceTypes.component.css'
    ],
    providers: [
        ValidSourceTypesService
    ]
})
export class ValidSourceTypesComponent implements OnInit {
    @Input() title = 'Valid Source Types';
    @Input() resource = 'constraints';

    validType = ToscaTypes.NodeType;

    loading: boolean;
    sortedValidTypes: SelectData[];
    allPossibleValidTypes: SelectData[];
    initialActiveItem: Array<SelectData>;
    currentSelectedItem: QNameApiData;
    validSourceTypes: ValidSourceTypesApiData = new ValidSourceTypesApiData();
    @ViewChild('addModal') addModal: ModalDirective;
    addModalRef: BsModalRef;
    columns: Array<WineryTableColumn> = [
        { title: 'Name', name: 'localname', sort: true },
        { title: 'Namespace', name: 'namespace', sort: true }
    ];

    constructor(public sharedData: InstanceService,
                protected service: ValidSourceTypesService,
                protected notify: WineryNotificationService,
                protected modalService: BsModalService) {
    }

    ngOnInit(): void {
        this.loading = true;
        forkJoin(
            this.service.getAvailableGenericType(this.validType),
            this.service.getValidSourceTypes(this.resource)
        ).subscribe(
            ([available, current]) => {
                this.loading = false;
                this.handleData(available);
                this.handleValidSourceTypesData(current);
            },
            error => this.handleError(error)
        );
    }

    saveToServer() {
        this.loading = true;
        this.service
            .saveValidSourceTypes(this.validSourceTypes, this.resource)
            .subscribe(() => {
                    this.loading = false;
                    this.notify.success('Saved changes.');
                },
                error => this.handleError(error));
    }

    onAddValidSourceType() {
        this.validSourceTypes.nodes.push(this.currentSelectedItem);
        this.handleValidSourceTypesChanged();
    }

    onAddClick() {
        this.addModalRef = this.modalService.show(this.addModal);
    }

    onRemoveClicked(selected: QNameApiData) {
        if (selected) {
            this.validSourceTypes.nodes = this.validSourceTypes.nodes.filter(item => item !== selected);
            this.handleValidSourceTypesChanged();
        }
    }

    onSelectedValueChanged(value: SelectData) {
        if (value.id !== null && value.id !== undefined) {
            this.currentSelectedItem = QNameApiData.fromQName(QName.stringToQName(value.id));
        } else {
            this.currentSelectedItem = null;
        }
    }

    handleValidSourceTypesChanged() {
        if (this.allPossibleValidTypes) {
            this.sortedValidTypes = this.allPossibleValidTypes
                .map(parentNode => {
                    if (parentNode.children) {
                        const children = parentNode.children.filter(node => {
                            const asQName = QName.stringToQName(node.id);
                            const existing: QNameApiData = this.validSourceTypes.nodes.find(qname => qname.localname === asQName.localName
                                && qname.namespace === asQName.nameSpace);
                            return existing === null || existing === undefined;
                        });
                        if (children !== undefined && children.length > 0) {
                            return { id: parentNode.id, text: parentNode.text, children: children };
                        }
                    }
                    return null;
                })
                .filter(item => item !== null);
            if (this.sortedValidTypes !== null
                && this.sortedValidTypes !== undefined
                && this.sortedValidTypes.length > 0
                && this.sortedValidTypes[0].children.length > 0) {
                this.initialActiveItem = [this.sortedValidTypes[0].children[0]];
                this.onSelectedValueChanged(this.initialActiveItem[0]);
            }
        }
    }

    handleData(nodeTypes: SelectData[]) {
        this.allPossibleValidTypes = nodeTypes;
        this.sortedValidTypes = this.allPossibleValidTypes;
    }

    handleValidSourceTypesData(data: ValidSourceTypesApiData) {
        if (data.nodes === null || data.nodes === undefined) {
            data.nodes = [];
        }
        this.validSourceTypes = data;
        this.handleValidSourceTypesChanged();
    }

    protected handleError(error: HttpErrorResponse): void {
        this.loading = false;
        this.notify.error(error.message, 'Error');
    }

}
