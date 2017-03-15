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
import { PropertiesDefinitionComponent } from './propertiesDefinition.component';
import { LoaderModule } from '../../loader/loader.module';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { SelectModule } from 'ng2-select';
import { TabsModule, TypeaheadModule } from 'ng2-bootstrap';
import { CommonModule } from '@angular/common';
import { WineryModalModule } from '../../wineryModalModule/winery.modal.module';
import { WineryTableModule } from '../../wineryTableModule/wineryTable.module';
import { DuplicateValidatorModule } from '../../validators/duplicateValidator.module';
import { NamespaceSelectorModule } from '../../namespaceSelector/namespaceSelector.module';

@NgModule({
    imports: [
        TabsModule.forRoot(),
        SelectModule,
        BrowserModule,
        FormsModule,
        LoaderModule,
        CommonModule,
        NamespaceSelectorModule,
        WineryModalModule,
        WineryTableModule,
        DuplicateValidatorModule,
    ],
    exports: [],
    declarations: [
        PropertiesDefinitionComponent,
    ],
    providers: [],
})
export class PropertiesDefinitionModule {
}
