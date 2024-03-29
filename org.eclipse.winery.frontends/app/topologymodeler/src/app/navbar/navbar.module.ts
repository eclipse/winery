/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
import { ModuleWithProviders, NgModule } from '@angular/core';
import { NavbarComponent } from './navbar.component';
import { CommonModule } from '@angular/common';
import { WineryFeatureToggleModule } from '../../../../tosca-management/src/app/wineryFeatureToggleModule/winery-feature-toggle.module';
import { BsDropdownModule, TooltipModule } from 'ngx-bootstrap';

@NgModule({
    imports: [
        CommonModule,
        WineryFeatureToggleModule,
        TooltipModule,
        BsDropdownModule

    ],
    declarations: [
        NavbarComponent
    ],
    exports: [
        NavbarComponent,
    ],
})
export class NavbarModule {

    static forRoot(): ModuleWithProviders {
        return {
            ngModule: NavbarModule,
            providers: [
            ]
        };
    }

}
