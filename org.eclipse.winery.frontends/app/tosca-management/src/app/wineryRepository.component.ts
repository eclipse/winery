/*******************************************************************************
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
 *******************************************************************************/
import { Component, OnInit, ViewChild } from '@angular/core';
import { WineryNotificationService } from './wineryNotificationModule/wineryNotification.service';
import { ExistService } from './wineryUtils/existService';
import { BackendAvailabilityStates } from './model/enums';
import { WineryGitLogComponent } from './wineryGitLog/wineryGitLog.component';
import { WineryRepositoryConfigurationService } from './wineryFeatureToggleModule/WineryRepositoryConfiguration.service';

@Component({
    selector: 'winery-repository',
    templateUrl: './wineryRepository.html',
    styleUrls: ['./wineryRepository.component.css'],
    providers: [
        ExistService, WineryRepositoryConfigurationService
    ]
})
/*
 * This component represents the root component for the Winery Repository.
 */
export class WineryRepositoryComponent implements OnInit {

    // region variables
    name = 'Winery Repository';
    backendState = BackendAvailabilityStates.Undefined;
    backendAvailabilityStates = BackendAvailabilityStates;
    loading = true;
    @ViewChild('gitLog') gitLog: WineryGitLogComponent;

    // endregion
    options = {
        position: ['top', 'right'],
        timeOut: 3000,
        lastOnBottom: true
    };

    constructor(private notify: WineryNotificationService, private existService: ExistService,
                private configurationService: WineryRepositoryConfigurationService) {
    }

    ngOnInit() {
        this.configurationService.getConfigurationFromBackend()
            .subscribe(data => {
                    this.backendState = BackendAvailabilityStates.Available;
                    this.loading = false;
                },
                error => {
                    this.loading = false;
                    this.backendState = BackendAvailabilityStates.Unavailable;
                }
            );
    }

    onClick() {
        this.gitLog.hide();
    }

    refresh() {
        window.location.reload();
    }

}
