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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { backendBaseURL } from '../configuration';
import { NamespaceWithPrefix } from '../wineryInterfaces/namespaceWithPrefix';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable()
export class WineryNamespaceSelectorService {

    constructor(private http: HttpClient) {
    }

    getNamespaces(all: boolean = false): Observable<NamespaceWithPrefix[]> {
        const headers = new HttpHeaders({ 'Accept': 'application/json' });

        let URL: string;
        if (all) {
            URL = backendBaseURL + '/admin/namespaces/?all';
        } else {
            URL = backendBaseURL + '/admin/namespaces/';
        }

        return this.http.get<NamespaceWithPrefix[]>(URL, { headers: headers });
    }
}
