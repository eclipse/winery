/********************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BsDropdownModule, TooltipModule, TypeaheadModule } from 'ngx-bootstrap';
import { ToastrModule } from 'ngx-toastr';
import { NgReduxModule } from '@angular-redux/store';
import { RouterModule } from '@angular/router';
import { PropertiesComponent } from './properties.component';
import { KeysPipe } from '../pipes/keys.pipe';
import { WineryModalModule } from '../../../../tosca-management/src/app/wineryModalModule/winery.modal.module';
import { KvPropertiesComponent } from './kv-properties/kv-properties.component';
import { XmlPropertiesComponent } from './xml-properties/xml-properties.component';
import { YamlPropertiesComponent } from './yaml-properties/yaml-properties.component';
import { ConstraintChecking, ConstraintClause } from './property-constraints';
import { TypeawareInputComponent } from './yaml-properties/typeaware-input.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        HttpClientModule,
        BrowserAnimationsModule,
        BsDropdownModule.forRoot(),
        ToastrModule.forRoot(),
        NgReduxModule,
        RouterModule,
        WineryModalModule,
        TypeaheadModule.forRoot(),
        TooltipModule.forRoot(),
    ],
    declarations: [
        PropertiesComponent,
        KvPropertiesComponent,
        XmlPropertiesComponent,
        YamlPropertiesComponent,
        TypeawareInputComponent,
        KeysPipe,
    ],
    exports: [
        PropertiesComponent,
        KeysPipe
    ]
})
export class PropertiesModule {

}
