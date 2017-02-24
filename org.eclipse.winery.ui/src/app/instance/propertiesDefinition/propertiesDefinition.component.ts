/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Lukas Harzentter - initial API and implementation
 *******************************************************************************/

import { Component, OnInit } from '@angular/core';
import { InstanceService } from '../instance.service';

@Component({
    selector: 'winery-instance-propertyDefinition',
    templateUrl: 'propertiesDefinition.component.html'
})
export class PropertyDefinitionComponent implements OnInit {

    xsdelement: string;
    xsdtype: string;

    constructor(private sharedData: InstanceService) {
    }

    ngOnInit() {
    }

    onNoneSelected(): void {
        console.log('none');
    }
    onXmlElementSelected(): void {
        console.log('xml elmeent');
    }
    onXmlTypeSelected(): void {
        console.log('xml t');
    }
    onCustomKeyValuePairSelected(): void {
        console.log('cuzstiom');
    }
}
