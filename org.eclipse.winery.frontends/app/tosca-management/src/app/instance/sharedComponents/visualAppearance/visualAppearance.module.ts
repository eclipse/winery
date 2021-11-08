/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {TabsModule} from 'ngx-bootstrap';
import {WineryLoaderModule} from '../../../wineryLoader/wineryLoader.module';
import {WineryModalModule} from '../../../wineryModalModule/winery.modal.module';
import {VisualAppearanceComponent} from './visualAppearance.component';
import {WineryUploaderModule} from '../../../wineryUploader/wineryUploader.module';

/**
 * An angular module for displaying the visualApperances for both the nodeTypes and the relationshipTypes
 * therefore an instance of {@link NodeTypesVisualsApiData} or {@link RelationshipTypesVisualsApiData} is loaded from the backend
 */
@NgModule({
    imports: [
        TabsModule.forRoot(),
        BrowserModule,
        FormsModule,
        WineryLoaderModule,
        CommonModule,
        WineryModalModule,
        WineryUploaderModule
    ],
    exports: [],
    declarations: [
        VisualAppearanceComponent,
    ]
})
export class VisualAppearanceModule {
}
