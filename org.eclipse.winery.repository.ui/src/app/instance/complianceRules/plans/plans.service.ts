/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
 ********************************************************************************/
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Headers, Http, RequestOptions, Response } from '@angular/http';
import { Observable } from 'rxjs';
import { PlansApiData } from './plansApiData';
import { backendBaseURL } from '../../../configuration';
import { SelectData } from '../../../wineryInterfaces/selectData';

@Injectable()
export class PlansService {

    path: string;

    constructor(private http: Http,
                private route: Router) {
        this.path = backendBaseURL + this.route.url + '/';
    }

    getPlansData(url?: string): Observable<PlansApiData[]> {
        return this.getJson(url ? backendBaseURL + url : this.path);
    }

    getPlanTypes(): Observable<SelectData[]> {
        return this.getJson(backendBaseURL + '/admin/plantypes/?ngSelect=true');
    }

    getPlanLanguages(): Observable<SelectData[]> {
        return this.getJson(backendBaseURL + '/admin/planlanguages/?ngSelect=true');
    }

    addPlan(newPlan: PlansApiData): Observable<Response> {
        const headers = new Headers({'Content-Type': 'application/json'});
        const options = new RequestOptions({headers: headers});

        return this.http.post(this.path, JSON.stringify(newPlan), options);
    }

    deletePlan(id: string): Observable<Response> {
        return this.http.delete(this.path + id + '/');
    }

    private getJson(path: string): Observable<any> {
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});

        return this.http.get(path, options)
            .map(res => res.json());
    }

    updatePlan(plan: PlansApiData): Observable<Response> {
        const headers = new Headers({'Content-Type': 'application/json'});
        const options = new RequestOptions({headers: headers});

        return this.http.put(this.path + plan.id + '/', JSON.stringify(plan), options);
    }
}
