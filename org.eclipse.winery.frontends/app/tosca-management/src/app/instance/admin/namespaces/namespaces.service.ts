/*******************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
import { Injectable } from '@angular/core';
import { WineryNamespaceSelectorService } from '../../../wineryNamespaceSelector/wineryNamespaceSelector.service';
import { Observable } from 'rxjs';
import { backendBaseURL } from '../../../configuration';
import { Router } from '@angular/router';
import { NamespaceProperties } from '../../../model/namespaceProperties';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';

@Injectable()
export class NamespacesService {

    private readonly path = '/admin/namespaces';

    constructor(private http: HttpClient,
                private namespaceService: WineryNamespaceSelectorService) {
    }

    getAllNamespaces(): Observable<NamespaceProperties[]> {
        return this.namespaceService.getNamespaces(true);
    }

    postNamespaces(namespaces: NamespaceProperties[]): Observable<HttpResponse<string>> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        return this.http.post<string>(
            backendBaseURL + this.path + '/',
            JSON.stringify(namespaces),
            { headers: headers, observe: 'response' }
        );
    }

}
