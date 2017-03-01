/**
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Lukas Harzenetter - initial API and implementation
 */

import { NgModule } from '@angular/core';

import { PropertiesDefinitionComponent }   from './propertiesDefinition.component';
import { MdTabsModule } from '@angular/material';
import { LoaderModule } from '../../loader/loader.module';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { SelectModule } from 'ng2-select';
import { TabsModule } from 'ng2-bootstrap';
import { Ng2TableModule } from 'ng2-table';
import { PaginationModule } from 'ng2-bootstrap';
// import { NgTableComponent, NgTableFilteringDirective, NgTablePagingDirective, NgTableSortingDirective } from 'ng2-table/ng2-table';



@NgModule({
    imports: [
        PaginationModule.forRoot(),
        Ng2TableModule,
        TabsModule.forRoot(),
        SelectModule,
        BrowserModule,
        FormsModule,
        LoaderModule,
        MdTabsModule
    ],
    exports: [],
    declarations: [PropertiesDefinitionComponent],
    providers: [],
})
export class PropertiesDefinitionModule {
}
