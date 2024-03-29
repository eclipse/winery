/*******************************************************************************
 * Copyright (c) 2017-2023 Contributors to the Eclipse Foundation
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
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { NgxPaginationModule } from 'ngx-pagination';
import { SelectModule } from 'ng2-select';
import { EntityContainerComponent } from './entityContainer/entityContainer.component';
import { WineryLoaderModule } from '../wineryLoader/wineryLoader.module';
import { WineryNamespaceSelectorModule } from '../wineryNamespaceSelector/wineryNamespaceSelector.module';
import { WineryDuplicateValidatorModule } from '../wineryValidators/wineryDuplicateValidator.module';
import { WineryModalModule } from '../wineryModalModule/winery.modal.module';
import { SectionComponent } from './section.component';
import { SectionPipe } from './section.pipe';
import { WineryUploaderModule } from '../wineryUploader/wineryUploader.module';
import { ModalModule, TooltipModule } from 'ngx-bootstrap';
import { WineryPipesModule } from '../wineryPipes/wineryPipes.module';
import { XaasPackagerComponent } from './xaasPackager/xaasPackager.component';
import { WineryAddModule } from '../wineryAddComponentModule/addComponent.module';
import { TargetAllocationModule } from '../wineryTargetAllocation/targetAllocation.module';
import { EntityComponent } from './entityContainer/entity.component';
import { TagInputModule } from 'ngx-chips';
import { AdminModule } from '../wineryMainModules/admin/admin.module';
import { WineryFileComparisonModule } from '../wineryFileComparisonModule/wineryFileComparison.module';
import { PlacementComponent } from './placement/placement.component';
import { WineryFeatureToggleModule } from '../wineryFeatureToggleModule/winery-feature-toggle.module';

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        CommonModule,
        NgxPaginationModule,
        WineryDuplicateValidatorModule,
        WineryNamespaceSelectorModule,
        WineryLoaderModule,
        WineryModalModule,
        RouterModule,
        SelectModule,
        WineryUploaderModule,
        TooltipModule,
        WineryPipesModule,
        WineryFeatureToggleModule,
        TagInputModule,
        WineryAddModule,
        AdminModule, // to be able to access the ConfigurationService and AccountabilityService
        WineryFileComparisonModule,
        ModalModule,
        TargetAllocationModule
    ],
    exports: [SectionComponent],
    declarations: [
        SectionComponent,
        EntityComponent,
        EntityContainerComponent,
        SectionPipe,
        XaasPackagerComponent,
        PlacementComponent
    ],
})
export class SectionModule {
}
