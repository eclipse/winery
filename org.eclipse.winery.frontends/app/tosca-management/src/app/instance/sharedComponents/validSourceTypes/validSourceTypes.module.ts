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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { SelectModule } from 'ng2-select';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { WineryLoaderModule } from '../../../wineryLoader/wineryLoader.module';
import { WineryQNameSelectorModule } from '../../../wineryQNameSelector/wineryQNameSelector.module';
import { ValidSourceTypesComponent } from './validSourceTypes.component';
import { WineryModalModule } from '../../../wineryModalModule/winery.modal.module';
import { WineryTableModule } from '../../../wineryTableModule/wineryTable.module';

@NgModule({
    imports: [
        CommonModule,
        BrowserModule,
        SelectModule,
        FormsModule,
        CommonModule,
        RouterModule,
        WineryLoaderModule,
        WineryQNameSelectorModule,
        WineryModalModule,
        WineryTableModule
    ],
    exports: [ValidSourceTypesComponent],
    declarations: [ValidSourceTypesComponent],
    providers: [],
})
export class ValidSourceTypesModule {
}
