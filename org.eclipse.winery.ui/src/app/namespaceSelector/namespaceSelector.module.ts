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

import { NamespaceSelectorComponent }   from './namespaceSelector.component';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { TypeaheadModule } from 'ng2-bootstrap';
import { LoaderModule } from '../loader/loader.module';
import { ToastModule } from 'ng2-toastr';

@NgModule({
    imports: [
        FormsModule,
        BrowserModule,
        TypeaheadModule,
        LoaderModule,
        ToastModule.forRoot(),
    ],
    exports: [NamespaceSelectorComponent],
    declarations: [NamespaceSelectorComponent],
    providers: [],
})
export class NamespaceSelectorModule {
}
