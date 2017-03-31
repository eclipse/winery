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
 *     Niko Stadelmaier - add admin component
 */
import { NgModule } from '@angular/core';
import { InstanceComponent } from './instance.component';
import { EditXMLComponent } from './editXML/editXML.component';
import { VisualAppearanceComponent } from './visualAppearance/visualAppearance.component';
import { DocumentationComponent } from './documentation/documentation.component';
import { InheritanceComponent } from './inheritance/inheritance.component';
import { InterfacesComponent } from './interfaces/interfaces.component';
import { ImplementationsComponent } from './implementations/implementations.component';
import { RequirementDefinitionsComponent } from './requirementDefinitions/requirementDefinitions.component';
import { InstanceStatesComponent } from './instanceStates/instanceStates.component';
import { CapabilityDefinitionsComponent } from './capabilityDefinitions/capabilityDefinitions.component';
import { PropertiesDefinitionComponent } from './propertiesDefinition/propertiesDefinition.component';
import { InstanceResolver } from './instance.resolver';
import { AppliesToComponent } from './appliesTo/appliesTo.component';
import { LanguageComponent } from './language/language.component';
import { TopologyTemplateComponent } from './topologyTemplate/topologyTemplate.component';
import { PlansComponent } from './plans/plans.component';
import { SelfservicePortalComponent } from './selfservicePortal/selfservicePortal.component';
import { BoundaryDefinitionsComponent } from './boundaryDefinitions/boundaryDefinitions.component';
import { ValidSourcesAndTargetsComponent } from './validSourcesAndTargets/validSourcesAndTargets.component';
import { FilesComponent } from './filesTag/files.component';
import { PropertiesComponent } from './properties/properties.component';
import { RequiredCapabilityTypeComponent } from './requiredCapabilityType/requiredCapabilityType.component';
import { ImplementationArtifactsComponent } from './implementationArtifacts/implementationArtifacts.component';
import { DeploymentArtifactsComponent } from './deploymentArtifacts/deploymentArtifacts.component';
import { LoggerComponent } from './logger/logger.component';
import { PlanLanguagesComponent } from './planLanguages/planLanguages.component';
import { NamespacesComponent } from './namespaces/namespaces.component';
import { RepositoryComponent } from './repository/repository.component';
import { ConstraintTypesComponent } from './contraintTypes/constraintTypes.component';
import { PlanTypesComponent } from './planTypes/planTypes.component';
import { TagComponent } from './tag/tag.component';
import { RouterModule } from '@angular/router';


const instanceRoutes = [

    {
        path: 'admin',
        component: InstanceComponent,
        resolve: { resolveData: InstanceResolver },
        children: [
            { path: 'namespaces', component: NamespacesComponent },
            { path: 'repository', component: RepositoryComponent },
            { path: 'planlanguages', component: PlanLanguagesComponent },
            { path: 'plantypes', component: PlanTypesComponent },
            { path: 'constrainttypes', component: ConstraintTypesComponent },
            { path: 'log', component: LoggerComponent },
            { path: '', redirectTo: 'namespaces', pathMatch: 'full'}
        ]
    },
    {
        path: ':section/:namespace/:instanceId',
        component: InstanceComponent,
        resolve: { resolveData: InstanceResolver },
        children: [
            { path: 'appliesto', component: AppliesToComponent },
            { path: 'boundarydefinitions', component: BoundaryDefinitionsComponent },
            { path: 'capabilitydefinitions', component: CapabilityDefinitionsComponent },
            { path: 'deploymentartifacts', component: DeploymentArtifactsComponent },
            { path: 'documentation', component: DocumentationComponent },
            { path: 'files', component: FilesComponent },
            { path: 'implementationartifacts', component: ImplementationArtifactsComponent },
            { path: 'implementations', component: ImplementationsComponent },
            { path: 'inheritance', component: InheritanceComponent },
            { path: 'instancestates', component: InstanceStatesComponent },
            { path: 'interfaces', component: InterfacesComponent },
            { path: 'language', component: LanguageComponent },
            { path: 'plans', component: PlansComponent },
            { path: 'properties', component: PropertiesComponent },
            { path: 'propertiesdefinition', component: PropertiesDefinitionComponent },
            { path: 'requiredcapabilitytype', component: RequiredCapabilityTypeComponent },
            { path: 'requirementdefinitions', component: RequirementDefinitionsComponent },
            { path: 'selfserviceportal', component: SelfservicePortalComponent },
            { path: 'sourceinterfaces', component: InterfacesComponent },
            { path: 'targetinterfaces', component: InterfacesComponent },
            { path: 'tags', component: TagComponent},
            { path: 'topologytemplate', component: TopologyTemplateComponent },
            { path: 'validsourcesandtargets', component: ValidSourcesAndTargetsComponent },
            { path: 'visualappearance', component: VisualAppearanceComponent },
            { path: 'xml', component: EditXMLComponent }
        ]
    }
];

@NgModule({
    imports: [
        RouterModule.forChild(instanceRoutes)
    ],
    exports: [
        RouterModule
    ],
    providers: [
        InstanceResolver
    ],
})
export class InstanceRouterModule {
}
