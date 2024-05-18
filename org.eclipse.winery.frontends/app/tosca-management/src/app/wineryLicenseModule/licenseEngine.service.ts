/*******************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
import { interval, Observable, Subject } from 'rxjs';
import { InstanceService } from '../instance/instance.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { mergeMap, tap, filter, takeUntil } from 'rxjs/operators';

import {
    WineryRepositoryConfigurationService
} from '../wineryFeatureToggleModule/WineryRepositoryConfiguration.service';
import { License, Software, Status } from './LicenseEngineApiData';

@Injectable()
export class LicenseEngineService {
    stop = false;
    licenseEngineUrl = '';

    private software: Software;

    constructor(private http: HttpClient, private sharedData: InstanceService, private configurationService: WineryRepositoryConfigurationService) {
        this.licenseEngineUrl = this.configurationService.configuration.endpoints.licenseEngine;
    }

    getAllLicenses(): Observable<string[]> {
        const headers = new HttpHeaders({ 'Accept': 'application/json' });
        headers.append('Access-Control-Request-Method', 'GET');
        return this.http.get(this.licenseEngineUrl + '/licenses', {
            headers: headers, responseType: 'json'
        }).map(
            (data) => {
                const licenses = [];
                for (const item in data) {
                    if (data.hasOwnProperty(item)) {
                        licenses.push(data[item]);
                    }
                }
                return licenses;
            });
    }

    getLicenseTextEngine(name: string): Observable<string> {
        const headers = new HttpHeaders({ 'Accept': 'text/html' });
        headers.append('Access-Control-Request-Method', 'GET');
        return this.http.get(this.licenseEngineUrl + '/licenses/' + name + '/text', {
            headers: headers, responseType: 'text'
        }).map((response) => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(response, 'text/html');
            return doc.body.innerText;
        });
    }

    deleteId(): Observable<boolean> {
        const headers = new HttpHeaders({ 'content-type': 'application/json' });
        headers.append('Access-Control-Request-Method', 'DELETE');
        return this.http.delete(this.licenseEngineUrl + '/software/' + this.software.id, {
            headers: headers, observe: 'response'
        }).map((response) => {
            return response.status === 204;
        });
    }

    postSoftware(): Observable<boolean> {
        const headers = new HttpHeaders({ 'content-type': 'application/json' });
        headers.append('Access-Control-Request-Method', 'POST');
        return this.http.post(this.licenseEngineUrl + '/software', {
            'name': this.software.id, 'id': this.software.id, 'url': this.software.url, 'branch': this.software.branch
        }, { 'headers': headers, observe: 'response' }).map((response) => {
            return response.status === 202;
        });
    }

    getSourceCodeLicense(): Observable<Software> {
        const headers = new HttpHeaders({ 'Accept': 'application/json' });
        return this.http.get<Software>(this.licenseEngineUrl + '/software/' + this.software.id, {
            headers: headers
        });
    }

    getLicenseInformation(license: string): Observable<License> {
        const headers = new HttpHeaders({ 'Accept': 'application/json' });
        return this.http.get<License>(this.licenseEngineUrl + '/licenses/' + license, {
            headers: headers
        });
    }

    getCompatibleLicenses(): Observable<string[]> {
        const headers = new HttpHeaders({ 'content-type': 'application/json', 'Accept': 'application/json' });
        return this.http.get(this.licenseEngineUrl + '/software/' + this.software.id + '/compatible-licenses', {
            headers: headers
        }).map(
            (data) => {
                const licenses = [];
                for (const item in data) {
                    if (data.hasOwnProperty(item)) {
                        licenses.push(data[item]);
                    }
                }
                return licenses;
            });
    }

    poll(): Observable<Software> {
        this.stop = false;
        const pollStop = new Subject();
        return interval(5000).pipe(
            mergeMap(() => this.getSourceCodeLicense()),
            filter((software) => this.checkPollingCondition(software)),
            tap(() => {
                pollStop.next();
            }),
            takeUntil(pollStop)
        );
    }

    checkPollingCondition(software: Software): boolean {
        this.software = software;
        return this.isFinished() || this.isFailed() || this.stop;
    }

    createSoftware(id: string, url: string, branch: string) {
        this.software = {} as Software;
        this.software.url = url;
        this.software.branch = branch;
        this.software.id = id;
        this.software.status = Status.QUEUED;
    }

    getStatus() {
        return this.software.status;
    }

    isFinished(): boolean {
        if (this.software) {
            return this.software.status.valueOf() === Status.FINISHED.valueOf();
        }
        return false;
    }

    failed() {
        this.software.status = Status.FAILED;
    }

    isFailed() {
        if (this.software) {
            return this.software.status.valueOf() === Status.FAILED.valueOf();
        }
        return false;
    }

    resetLicenseData() {
        this.software = null;
        this.stop = true;
    }

    getFoundLicenses(): string[] {
        return this.software.licensesEffective;
    }

    getLicensewithFiles(id: String): any {
        const headers = new HttpHeaders({ 'Accept': 'application/json' });
        return this.http.get(this.licenseEngineUrl + '/software/' + id + '/licenses?effective=false', {
            headers: headers
        });
    }

    excludeFile(filename: String[]) {

        const headers = new HttpHeaders({ 'content-type': 'application/json' });
        headers.append('Access-Control-Request-Method', 'DELETE');
        return this.http.put(this.licenseEngineUrl + '/software/' + this.software.id + '/files-excluded', filename, {
            headers: headers, observe: 'response'
        }).map((response) => {
            return response.status === 200;
        });
    }

    deleteExcludedFile() {
        const headers = new HttpHeaders({ 'content-type': 'application/json' });
        headers.append('Access-Control-Request-Method', 'DELETE');
        return this.http.delete(this.licenseEngineUrl + '/software/' + this.software.id + '/files-excluded', {
            headers: headers, observe: 'response'
        }).map((response) => {
            return response.status === 204;
        });




    }
}
