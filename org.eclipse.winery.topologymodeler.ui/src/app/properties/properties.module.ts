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
import { WineryModalModule } from '../../repositoryUiDependencies/wineryModalModule/winery.modal.module';
import { PropertiesComponent } from './properties.component';
import { PropertiesContentComponent } from './properties-content/properties-content.component';
import { KeysPipe } from '../pipes/keys.pipe';

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
        PropertiesContentComponent,
        KeysPipe
    ],
    exports: [
        PropertiesComponent,
        PropertiesContentComponent,
        KeysPipe
    ]
})
export class PropertiesModule {

}
