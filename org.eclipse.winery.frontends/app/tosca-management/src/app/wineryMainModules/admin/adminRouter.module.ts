/*******************************************************************************
 * Copyright (c) 2017-2019 Contributors to the Eclipse Foundation
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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SectionResolver } from '../../section/section.resolver';
import { InstanceComponent } from '../../instance/instance.component';
import { InstanceResolver } from '../../instance/instance.resolver';
import { ToscaTypes } from '../../model/enums';
import { RepositoryComponent } from '../../instance/admin/repository/repository.component';
import { TypeWithShortNameComponent } from '../../instance/admin/typesWithShortName/typeWithShortName.component';
import { NamespacesComponent } from '../../instance/admin/namespaces/namespaces.component';
import { LoggerComponent } from '../../instance/admin/logger/logger.component';
import { ConsistencyCheckComponent } from '../../instance/admin/consistencyCheck/consistencyCheck.component';
import { AccountabilityComponent } from '../../instance/admin/accountability/accountability.component';
import { AuthorizationComponent } from '../../instance/admin/accountability/authorization/authorization.component';
import { AuthenticationComponent } from '../../instance/admin/accountability/authentication/authentication.component';
import { ConfigurationComponent } from '../../instance/admin/accountability/configuration/configuration.component';
import { ProvenanceComponent } from '../../instance/admin/accountability/provenance/provenance.component';
import { FeatureConfigurationComponent } from '../../instance/admin/configuration/configuration.component';
import { EdmmMappingsComponent } from '../../instance/admin/edmmMappings/edmmMappings.component';

const toscaType = ToscaTypes.Admin;

const adminRoutes: Routes = [
    {
        path: toscaType,
        component: InstanceComponent,
        children: [
            { path: 'namespaces', component: NamespacesComponent },
            { path: 'repository', component: RepositoryComponent },
            { path: 'planlanguages', component: TypeWithShortNameComponent },
            { path: 'plantypes', component: TypeWithShortNameComponent },
            { path: 'constrainttypes', component: TypeWithShortNameComponent },
            { path: 'consistencycheck', component: ConsistencyCheckComponent },
            {
                path: 'accountability',
                component: AccountabilityComponent,
                children: [
                    { path: 'authorization', component: AuthorizationComponent },
                    { path: 'authentication', component: AuthenticationComponent },
                    { path: 'provenance', component: ProvenanceComponent },
                    { path: 'configuration', component: ConfigurationComponent },
                    { path: '', redirectTo: 'authorization', pathMatch: 'full' }
                ]
            },
            { path: 'log', component: LoggerComponent },
            { path: 'configuration', component: FeatureConfigurationComponent },
            { path: '1to1edmmmappings', component: EdmmMappingsComponent },
            { path: 'edmmtypemappings', component: EdmmMappingsComponent },
            { path: '', redirectTo: 'namespaces', pathMatch: 'full' }
        ]
    },
];

@NgModule({
    imports: [
        RouterModule.forChild(adminRoutes),
    ],
    exports: [
        RouterModule
    ],
    providers: [
        SectionResolver,
        InstanceResolver
    ],
})
export class AdminRouterModule {
}
