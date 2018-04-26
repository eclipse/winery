/*******************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
import { Component, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Response } from '@angular/http';
import { Subscription } from 'rxjs';
import { InstanceService } from './instance.service';
import { WineryNotificationService } from '../wineryNotificationModule/wineryNotification.service';
import { backendBaseURL } from '../configuration';
import { RemoveWhiteSpacesPipe } from '../wineryPipes/removeWhiteSpaces.pipe';
import { ExistService } from '../wineryUtils/existService';
import { isNullOrUndefined } from 'util';
import { WineryInstance } from '../wineryInterfaces/wineryComponent';
import { ToscaTypes } from '../wineryInterfaces/enums';
import { ToscaComponent } from '../wineryInterfaces/toscaComponent';
import { Utils } from '../wineryUtils/utils';
import { WineryVersion } from '../wineryInterfaces/wineryVersion';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
    templateUrl: 'instance.component.html',
    providers: [
        InstanceService,
        RemoveWhiteSpacesPipe,
    ]
})
export class InstanceComponent implements OnDestroy {

    availableTabs: string[];
    toscaComponent: ToscaComponent;
    versions: WineryVersion[];
    typeUrl: string;
    typeId: string;
    typeOf: string;
    imageUrl: string;
    newVersionAvailable: boolean;
    editable = true;
    loadingVersions = true;
    loadingData = true;

    routeSub: Subscription;

    constructor(private route: ActivatedRoute,
                private router: Router,
                private service: InstanceService,
                private notify: WineryNotificationService, private existService: ExistService) {
        this.routeSub = this.route
            .data
            .subscribe(data => {
                    this.newVersionAvailable = false;
                    // For convenience, we accept editing already existing components  without versions
                    this.editable = true;
                    this.toscaComponent = data['resolveData'] ? data['resolveData'] : new ToscaComponent(ToscaTypes.Admin, '', '');

                    this.service.setSharedData(this.toscaComponent);

                    if (!isNullOrUndefined(this.toscaComponent)
                        && this.toscaComponent.toscaType !== ToscaTypes.Imports
                        && this.toscaComponent.toscaType !== ToscaTypes.Admin) {
                        if (this.toscaComponent.toscaType === ToscaTypes.NodeType) {
                            const img = backendBaseURL + this.service.path + '/visualappearance/50x50';
                            this.existService.check(img)
                                .subscribe(
                                    () => this.imageUrl = img,
                                    () => this.imageUrl = null,
                                );
                        }
                        this.service.getComponentData()
                            .subscribe(
                                compData => this.handleComponentData(compData)
                            );
                        this.getVersionInfo();
                    } else {
                        this.loadingVersions = false;
                        this.loadingData = false;
                        this.editable = this.toscaComponent.toscaType === ToscaTypes.Admin;
                    }

                    this.availableTabs = this.service.getSubMenuByResource();
                },
                error => this.handleError(error)
            );
    }

    private getVersionInfo() {
        this.service.getVersions()
            .subscribe(
                versions => this.handleVersions(versions),
                (error: Response) => {
                    if (error.status === 500) {
                        // needed because the git client sometimes throws an exception reading the repository:
                        // java.io.EOFException: Short read of block
                        this.getVersionInfo();
                    }
                }
            );
    }

    deleteComponent() {
        this.service.deleteComponent().subscribe(data => this.handleDelete(), error => this.handleError(error));
    }

    private handleComponentData(data: WineryInstance) {
        this.typeUrl = Utils.getTypeOfTemplateOrImplementation(this.toscaComponent.toscaType);

        if (!isNullOrUndefined(this.typeUrl)) {
            this.typeUrl = '/' + this.typeUrl;
            const tempOrImpl = data.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0];
            let qName: string[];

            if (!isNullOrUndefined(tempOrImpl.type)) {
                qName = tempOrImpl.type.slice(1).split('}');
                this.typeOf = 'Type: ';
            } else if (!isNullOrUndefined(tempOrImpl.nodeType)) {
                qName = tempOrImpl.nodeType.slice(1).split('}');
                this.typeOf = 'Implementation for ';
            } else if (!isNullOrUndefined(tempOrImpl.relationshipType)) {
                qName = tempOrImpl.relationshipType.slice(1).split('}');
                this.typeOf = 'Implementation for ';
            }

            if (qName.length === 2) {
                this.typeUrl += '/' + encodeURIComponent(qName[0]) + '/' + qName[1];
                this.typeId = qName[1];
            } else {
                this.typeUrl = null;
            }
        }

        this.loadingData = false;
    }

    private handleVersions(list: WineryVersion[]) {
        // create instances of class {@link WineryVersion}
        const versions: WineryVersion[] = [];
        for (const obj of list) {
            versions.push(
                new WineryVersion(
                    obj.componentVersion,
                    obj.wineryVersion,
                    obj.workInProgressVersion,
                    obj.currentVersion,
                    obj.latestVersion,
                    obj.releasable,
                    obj.editable)
            );
        }
        this.versions = this.service.versions = versions;
        this.loadingVersions = false;

        const version = this.versions.find(v => v.currentVersion);
        if (!isNullOrUndefined(version)) {
            this.service.currentVersion = version;
            this.newVersionAvailable = !version.latestVersion;
            this.editable = version.editable;
        }
    }

    private handleDelete() {
        this.notify.success('Successfully deleted ' + this.toscaComponent.localName);
        this.router.navigate(['/' + this.toscaComponent.toscaType]);
    }

    private handleError(error: HttpErrorResponse) {
        this.notify.error(error.message, 'Error');
    }

    ngOnDestroy(): void {
        this.routeSub.unsubscribe();
    }
}
