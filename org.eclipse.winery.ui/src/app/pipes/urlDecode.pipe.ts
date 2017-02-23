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

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'urlEncode'
})

export class UrlDecodePipe implements PipeTransform {
    transform(value: any, args: any[]): any {
        if (value) {
            return decodeURIComponent(value);
        }
    }
}
