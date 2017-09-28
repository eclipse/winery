/**
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v20.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 */
import { Injectable } from '@angular/core';
import { Headers, Http, RequestOptions } from '@angular/http';
import { InstanceService } from '../../instance.service';
import { Observable } from 'rxjs/Observable';
import { backendBaseURL } from '../../../configuration';
import { ArtifactResourceApiData } from '../artifactSource/ArtifactResourceApiData';

@Injectable()
export class ReadmeService {

    constructor(private http: Http,
                private sharedData: InstanceService) {
    }

    getData(): Observable<string> {
        const headers = new Headers({ 'Accept': 'text/plain' });
        const options = new RequestOptions({ headers: headers });
        return this.http.get(backendBaseURL + this.sharedData.path + '/source/README.md', options)
            .map(res => res.text());
    }

    save(readmeFile: ArtifactResourceApiData) {
        const headers = new Headers({ 'Content-Type': 'application/json' });
        const options = new RequestOptions({ headers: headers });

        return this.http.put(backendBaseURL + this.sharedData.path + '/source/README.md', readmeFile, options)
            .map(res => res.json());
    }

}
