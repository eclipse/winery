/*******************************************************************************
 * Copyright (c) 2017-2021 Contributors to the Eclipse Foundation
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
import { Component, OnInit, ViewChild } from '@angular/core';
import { WineryRowData, WineryTableColumn } from '../../../wineryTableModule/wineryTable.component';
import { PlansApiData } from './plansApiData';
import { PlansService } from './plans.service';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { SelectData } from '../../../model/selectData';
import { WineryUploaderComponent } from '../../../wineryUploader/wineryUploader.component';
import { SelectItem } from 'ng2-select';
import { InterfaceParameter } from '../../../model/parameters';
import { backendBaseURL } from '../../../configuration';
import { InstanceService } from '../../instance.service';
import { BsModalRef, BsModalService } from 'ngx-bootstrap';
import { WineryRepositoryConfigurationService } from '../../../wineryFeatureToggleModule/WineryRepositoryConfiguration.service';
import { InterfacesService } from '../../sharedComponents/interfaces/interfaces.service';
import { InterfaceOperationApiData, InterfacesApiData } from '../../sharedComponents/interfaces/interfacesApiData';
import { PlanOperation } from '../../sharedComponents/interfaces/targetInterface/operations';
import { YesNoEnum } from '../../../model/enums';
import { HttpErrorResponse } from '@angular/common/http';

const bpmn4tosca = 'http://www.opentosca.org/bpmn4tosca';

@Component({
    selector: 'winery-plans',
    templateUrl: 'plans.component.html',
    providers: [
        PlansService
    ]
})
export class PlansComponent implements OnInit {

    loading = true;

    embeddedPlansColumns: Array<WineryTableColumn> = [
        { title: 'Precondition', name: 'precondition', sort: true },
        { title: 'Name', name: 'name', sort: true },
        { title: 'Type', name: 'planType', sort: true },
        { title: 'Language', name: 'planLanguage', sort: true }
    ];
    linkedPlansColumns: Array<WineryTableColumn> = [
        { title: 'Precondition', name: 'precondition', sort: true },
        { title: 'Name', name: 'name', sort: true },
        { title: 'Type', name: 'planType', sort: true },
        { title: 'Language', name: 'planLanguage', sort: true },
        { title: 'Reference', name: 'planModelReference.reference', sort: true }
    ];

    plansApiData: PlansApiData[] = null;
    linkedPlans: any[] = null;
    elementToRemove: PlansApiData;
    enableEditButton = false;

    planTypes: SelectData[];
    selectedPlanType: SelectData = new SelectData();
    planLanguages: SelectData[];
    selectedPlanLanguage: SelectData = new SelectData();
    newPlan = new PlansApiData();
    fileDropped = false;
    showArchiveUpload = true;
    fileToUpload: any;
    uploaderUrl: string;
    interfaces = new InterfacesApiData();
    ioModalRef: BsModalRef;

    @ViewChild('addPlanModal') addPlanModal: any;
    @ViewChild('uploader') uploader: WineryUploaderComponent;

    @ViewChild('ioModal') ioModal: any;
    @ViewChild('confirmDeleteModal') confirmDeleteModal: any;

    @ViewChild('confirmPlanGeneration') confirmPlanGeneration: any;

    constructor(private notify: WineryNotificationService,
                public sharedData: InstanceService,
                private service: PlansService,
                private modalService: BsModalService,
                private interfaceService: InterfacesService,
                private configurationService: WineryRepositoryConfigurationService) {
    }

    ngOnInit() {
        this.getPlanTypesData();
        this.uploaderUrl = this.service.path + 'addarchive/';
    }

    // region ########## Plan Generation ##########
    onGeneratePlans() {
        this.confirmPlanGeneration.show();
    }

    generatePlans() {
        this.loading = true;
        this.service.generatePlans().subscribe(
            () => this.handlePlanSaved(),
            error => {
                this.loading = false;
                this.notify.warning(
                    'Plan Builder service is not available or raised an error:\n' + error.message,
                    'Warning: No Plans Generated'
                );
            }
        );
    }

    // endregion

    // region ########## Callbacks ##########
    // region ########## Table ##########
    onAddPlanType() {
        this.refreshPlanLanguages();
        this.refreshPlanTypes();
        this.newPlan = new PlansApiData();
        this.fileToUpload = null;
        this.fileDropped = false;
        this.showArchiveUpload = true;
    }

    onEditPlan(plan: PlansApiData) {
        if (plan.planLanguage.includes('BPMN')) {
            const bpmnUrl = this.configurationService.configuration.endpoints.bpmnModeler
                + '?repositoryURL=' + encodeURIComponent(backendBaseURL + '/')
                + '&namespace=' + encodeURIComponent(this.sharedData.toscaComponent.namespace)
                + '&id=' + this.sharedData.toscaComponent.localName
                + '&plan=' + plan.name;
            window.open(bpmnUrl, '_blank');
        } else if (plan.planLanguage.includes('bpel')) {
            // This allows the use of the bpmn Modeler for a feature, which shows the sequence
            // of a bpel file with the use of said modeler.
            // Therefore the bpmnModeler endpoint is correct.
            const bpelUrl = this.configurationService.configuration.endpoints.bpmnModeler
                + '?repositoryURL=' + encodeURIComponent(backendBaseURL + '/')
                + '&namespace=' + encodeURIComponent(this.sharedData.toscaComponent.namespace)
                + '&id=' + this.sharedData.toscaComponent.localName
                + '&plan=' + plan.name + '/bpel';
            window.open(bpelUrl, '_blank');
        } else {
            const workflowUrl = this.configurationService.configuration.endpoints.workflowmodeler
                + '?repositoryURL=' + encodeURIComponent(backendBaseURL + '/')
                + '&namespace=' + encodeURIComponent(this.sharedData.toscaComponent.namespace)
                + '&id=' + this.sharedData.toscaComponent.localName
                + '&plan=' + plan.name;
            window.open(workflowUrl, '_blank');
        }
    }

    onRemovePlan(plan: PlansApiData) {
        this.elementToRemove = plan;
        this.confirmDeleteModal.show();
    }

    onEditPlanIOParameters(selectedType: PlansApiData) {
        this.newPlan = selectedType;
        if (!(this.newPlan.inputParameters)) {
            this.newPlan.inputParameters = [];
        }
        if (!(this.newPlan.outputParameters)) {
            this.newPlan.outputParameters = [];
        }
        this.ioModalRef = this.modalService.show(this.ioModal);
    }

    onCellSelected(plan: WineryRowData) {
        const selected: PlansApiData = plan.row;
        this.enableEditButton = true;
    }
    // endregion

    // region ########## Add Modal ##########
    addPlan() {
        this.newPlan.planLanguage = this.selectedPlanLanguage.id;
        this.newPlan.planType = this.selectedPlanType.id;
        if (this.newPlan.planLanguage.includes('BPMN')) {
            const paramInstanceData = new InterfaceParameter('instanceDataAPIUrl', 'String', YesNoEnum.YES);
            const paramServiceInstanceData = new InterfaceParameter('OpenTOSCAContainerAPIServiceInstanceURL', 'String', YesNoEnum.YES);
            const paramCorrelation = new InterfaceParameter('CorrelationID', 'String', YesNoEnum.YES);
            this.newPlan.inputParameters = [paramInstanceData, paramServiceInstanceData, paramCorrelation];
        }
        this.service.addPlan(this.newPlan)
            .subscribe(
                () => this.handlePlanCreated(),
                (error) => this.handleError(error)
            );
    }

    refreshPlanTypes() {
        this.service.getPlanTypes()
            .subscribe(
                data => this.handlePlanTypes(data),
                error => this.handleError(error)
            );
    }

    refreshPlanLanguages() {
        this.service.getPlanLanguages()
            .subscribe(
                data => this.handlePlanLanguages(data),
                error => this.handleError(error)
            );
    }

    planLanguageSelected(event: SelectItem) {
        if (event.id.includes(bpmn4tosca) || event.id.includes('BPMN')) {
            this.fileDropped = true;
            this.showArchiveUpload = false;
        } else if (this.fileToUpload) {
            this.fileDropped = true;
            this.showArchiveUpload = true;
        } else {
            this.fileDropped = false;
            this.showArchiveUpload = true;
        }

        this.selectedPlanLanguage = event;
    }

    planTypeSelected(event: SelectItem) {
        this.selectedPlanType = event;
    }

    onFileDropped(event: any) {
        this.fileDropped = true;
        this.fileToUpload = event;
    }

    // endregion

    // region ######### IOParameter Modal #########
    editPlan() {
        this.service.updatePlan(this.newPlan)
            .subscribe(
                () => this.handlePlanSaved(),
                error => this.handleError(error)
            );
    }

    // endregion

    // region ########## Remove Modal ##########
    deletePlan() {
        const tempInterfaces = this.interfaceService.getInterfaces(this.service.path);
        const arr: InterfacesApiData[] = [];
        tempInterfaces.forEach((interfaces) => {
            for (let i = 0; i < interfaces.length; i++) {
                for (let j = 0; j < interfaces[i].operations.length; j++) {
                    if (interfaces[i].operations[j].plan !== null && interfaces[i].operations[j].plan.planRef !== this.elementToRemove.id) {
                        const interfaceNew = new InterfacesApiData(interfaces[i].id);
                        interfaceNew.id = interfaceNew.name;
                        interfaceNew.operations = [interfaces[i].operations[j]];
                        arr.push(interfaceNew);
                    }
                }
            }
            this.interfaceService.clear(this.service.path)
                .subscribe(
                    () => {this.interfaceService.save(arr)
                        .subscribe(
                            () => {this.service.deletePlan(this.elementToRemove.id)
                                .subscribe(
                                    () => {
                                        this.notify.success('Successfully deleted plan ' + this.elementToRemove.name);
                                        this.getPlanTypesData();
                                    },
                                    (error) => this.handleError(error)
                                ); },
                            (error) => this.handleError(error)
                        ); },
                    (error) => this.handleError(error)
                );
        });
    }

    getPlanTypesData() {
        this.service.getPlansData()
            .subscribe(
                data => this.handleData(data),
                error => this.handleError(error)
            );
    }

    handleError(error: HttpErrorResponse) {
        this.notify.error(error.message);
        this.loading = false;
    }
    // endregion
    // region ########## Private Methods ##########
    private handleData(data: PlansApiData[]) {
        this.plansApiData = data;
        this.loading = false;
    }

    private handlePlanTypes(types: SelectData[]) {
        this.planTypes = types;
        this.selectedPlanType = types[0] ? types[0] : new SelectData();

        if (this.planLanguages) {
            this.loading = false;
            this.addPlanModal.show();
        }
    }

    private handlePlanLanguages(languages: SelectData[]) {
        this.planLanguages = languages;
        this.selectedPlanLanguage = languages[0] ? languages[0] : new SelectData();

        if (this.planTypes) {
            this.loading = false;
            this.addPlanModal.show();
        }
    }

    private handlePlanCreated() {
        this.loading = true;
        this.uploaderUrl = this.service.path + this.newPlan.name + '/file';

        if (this.newPlan.planLanguage.includes('BPMN')) {
            const interfaceName = this.interfaces.id;
            const operationName = (<HTMLInputElement>document.getElementById('operationName')).value;
            const testInterface = new InterfacesApiData(interfaceName);
            testInterface.id = testInterface.name;
            const operation = new InterfaceOperationApiData();
            operation.name = operationName;
            operation.plan = new PlanOperation();
            operation.plan.planRef = this.newPlan.name;
            testInterface.operations = [operation];
            const tempInterfaces = this.interfaceService.getInterfaces(this.service.path);
            let arr: InterfacesApiData[] = [];
            let containsInterface, sameName = false;
            tempInterfaces.forEach((interfaces) => {
                arr = interfaces;
                for (let i = 0; i < arr.length; i++) {
                    if (arr[i].id === testInterface.id) {
                        sameName = true;
                        for (let j = 0; j < arr[i].operations.length; j++) {
                            if (arr[i].operations[j].name === testInterface.operations[0].name) {
                                arr[i].operations[j] = testInterface.operations[0];
                                containsInterface = true;
                            }
                        }
                        if (!containsInterface) {
                            arr[i].operations.push(testInterface.operations[0]);
                        }
                    }
                }
                if (!sameName) {
                    arr.push(testInterface);
                }
            });
            // clear interfaces before otherwise the interfaces tag is empty
            this.interfaceService.clear(this.service.path)
                .subscribe(
                    () => {this.interfaceService.save(arr)
                        .subscribe(
                            () => {this.notify.success('Successfully added interface.'); },
                            (error) => this.handleError(error)
                        ); },
                    (error) => this.handleError(error)
                );
        }
        if (!this.showArchiveUpload) {
            this.handlePlanSaved();
        } else {
            this.uploader.upload(this.uploaderUrl);
        }
    }

    private handlePlanSaved() {
        this.loading = false;
        this.notify.success('Successfully added Plan!');
        this.getPlanTypesData();
    }

    // endregion
}
