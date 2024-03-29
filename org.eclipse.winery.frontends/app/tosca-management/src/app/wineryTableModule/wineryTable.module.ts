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
import {PaginationModule} from 'ngx-bootstrap';
import {Ng2TableModule} from 'ng2-table';
import {WineryTableComponent} from './wineryTable.component';
import { RouterModule } from '@angular/router';

/**
 * This module must be imported in order to use the {@link WineryTableComponent}.
 * Documentation on how to use this component can also be found at the {@link WineryTableComponent}.
 */
@NgModule({
    imports: [
        PaginationModule.forRoot(),
        Ng2TableModule,
        BrowserModule,
        FormsModule,
        RouterModule,
    ],
    exports: [WineryTableComponent],
    declarations: [WineryTableComponent],
    providers: [],
})
export class WineryTableModule {
}
