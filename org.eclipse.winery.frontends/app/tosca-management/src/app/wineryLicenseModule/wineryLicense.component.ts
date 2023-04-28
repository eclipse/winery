/*******************************************************************************
 * Copyright (c) 2017-2022 Contributors to the Eclipse Foundation
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
import { ViewChild, Component, OnInit, TemplateRef } from '@angular/core';
import { WineryLicenseService } from './wineryLicense.service';
import { ChecklistDatabase } from './wineryLicenseTree.service';
import { WineryNotificationService } from '../wineryNotificationModule/wineryNotification.service';
import { InstanceService } from '../instance/instance.service';
import { ToscaTypes } from '../model/enums';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, map } from 'rxjs/internal/operators';
import { forkJoin } from 'rxjs';
import { Observable } from 'rxjs/Rx';
import {
    WineryRepositoryConfigurationService
} from '../wineryFeatureToggleModule/WineryRepositoryConfiguration.service';
import { saveAs } from 'file-saver';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatStepper } from '@angular/material/stepper';
import { BsModalRef, BsModalService } from 'ngx-bootstrap';
import { LicenseEngineService } from './licenseEngine.service';
import { License, LicenseTree, LicenseTreeFlatNode } from './LicenseEngineApiData';
import { MatTreeFlattener } from '@angular/material';
import { FlatTreeControl } from '@angular/cdk/tree';
import { MatTreeFlatDataSource } from '@angular/material/tree';
import { SelectionModel } from '@angular/cdk/collections';


@Component({
    templateUrl: 'wineryLicense.component.html',
    styleUrls: ['wineryLicense.component.css'],
    providers: [WineryLicenseService, LicenseEngineService, ChecklistDatabase]
})


export class WineryLicenseComponent implements OnInit {

    licenseID: String;
    loading = true;
    loadingLicense = false;
    isEditable = false;
    licenseAvailable = true;
    showForm = false;
    loadingbar = false;
    foundLicenses = '';
    foundLicenseswithFiles = new Map<string, Array<string>>();
    treeData = {};
    treeParentData = {};
    currentLicenseText = '';
    initialLicenseText = '';
    selectedLicenseText = '';
    licenseType = '';
    options: string[] = [];
    selectedOptions: string[] = [];
    compatibleLicenses: License[] = [];
    unknowLicense = [];
    nullLicense = [];
    toscaType: ToscaTypes;
    licenseEngine: boolean;
    firstFormGroup: FormGroup;
    secondFormGroup: FormGroup;
    confirmSaveModalRef: BsModalRef;
    confirmDownloadModalRef: BsModalRef;
    treeControl = new FlatTreeControl<LicenseTreeFlatNode>(
        (node) => node.level,
        (node) => node.expandable,
    );

    treeFlattener = new MatTreeFlattener(
        (node: LicenseTree, level_: number) => {
            return {
                expandable: !!node.files && node.files.length > 0,
                name: node.name,
                level: level_,
            };
        },
        (node) => node.level,
        (node) => node.expandable,
        (node) => node.files,
    );
    dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
    flatNodeMap = new Map<LicenseTreeFlatNode, LicenseTree>();
    nestedNodeMap = new Map<LicenseTree, LicenseTreeFlatNode>();
    /** A selected parent node to be inserted */
    selectedParent: LicenseTreeFlatNode | null = null;
    /** The new item's name */
    newItemName = '';
    /** The selection for checklist */
    checklistSelection = new SelectionModel<LicenseTreeFlatNode>(true /* multiple */);
    @ViewChild('stepper') stepper: MatStepper;
    @ViewChild('confirmSaveModal') confirmSaveModal: TemplateRef<any>;
    @ViewChild('confirmDownloadModal') confirmDownloadModal: TemplateRef<any>;

    constructor(private notify: WineryNotificationService,
                private configurationService: WineryRepositoryConfigurationService,
                private wlService: WineryLicenseService, private leService: LicenseEngineService,
                public sharedData: InstanceService, private formBuilder: FormBuilder,
                private modalService: BsModalService,
                private _database: ChecklistDatabase) {
        this.licenseEngine = configurationService.configuration.features.licenseEngine;
        this.toscaType = this.sharedData.toscaComponent.toscaType;

        this.treeFlattener = new MatTreeFlattener(this.transformer, this.getLevel,
            this.isExpandable, this.getChildren);
        this.treeControl = new FlatTreeControl<LicenseTreeFlatNode>(this.getLevel, this.isExpandable);
        this.dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

        _database.dataChange.subscribe((data) => {
            this.dataSource.data = data;
        });

    }




    getLevel = (node: LicenseTreeFlatNode) => node.level;
    isExpandable = (node: LicenseTreeFlatNode) => node.expandable;
    getChildren = (node: LicenseTree): LicenseTree[] => node.files;
    hasChild = (_: number, _nodeData: LicenseTreeFlatNode) => _nodeData.expandable;
    hasNoContent = (_: number, _nodeData: LicenseTreeFlatNode) => _nodeData.name === '';

    /**
     * Transformer to convert nested node to flat node. Record the nodes in maps for later use.
     */
    transformer = (node: LicenseTree, level: number) => {
        const existingNode = this.nestedNodeMap.get(node);
        // @ts-ignore
        // @ts-ignore
        const flatNode = existingNode && existingNode.item === node.item
            ? existingNode
            : new LicenseTreeFlatNode();
        flatNode.name = node.name;
        flatNode.level = level;
        flatNode.expandable = true;                   // edit this to true to make it always expandable
        this.flatNodeMap.set(flatNode, node);
        this.nestedNodeMap.set(node, flatNode);
        return flatNode;
    }

    /** Whether all the descendants of the node are selected. */
    descendantsAllSelected(node: LicenseTreeFlatNode): boolean {
        const descendants = this.treeControl.getDescendants(node);
        const descAllSelected = descendants.length > 0 && descendants.every((child) => {
            return this.checklistSelection.isSelected(child);
        });
        return descAllSelected;
    }

    /** Whether part of the descendants are selected */
    descendantsPartiallySelected(node: LicenseTreeFlatNode): boolean {
        const descendants = this.treeControl.getDescendants(node);
        const result = descendants.some((child) => this.checklistSelection.isSelected(child));
        return result && !this.descendantsAllSelected(node);
    }

    /** Toggle the to-do item selection. Select/deselect all the descendants node */
    todoItemSelectionToggle(node: LicenseTreeFlatNode): void {
        this.checklistSelection.toggle(node);
        const descendants = this.treeControl.getDescendants(node);
        this.checklistSelection.isSelected(node)
            ? this.checklistSelection.select(...descendants)
            : this.checklistSelection.deselect(...descendants);

        // Force update for the parent
        descendants.forEach((child) => this.checklistSelection.isSelected(child));
        this.checkAllParentsSelection(node);
    }
    /* Checks all the parents when a leaf node is selected/unselected */
    checkAllParentsSelection(node: LicenseTreeFlatNode): void {
        let parent: LicenseTreeFlatNode | null = this.getParentNode(node);
        while (parent) {
            this.checkRootNodeSelection(parent);
            parent = this.getParentNode(parent);
        }
    }

    /** Check root node checked state and change it accordingly */
    checkRootNodeSelection(node: LicenseTreeFlatNode): void {
        const nodeSelected = this.checklistSelection.isSelected(node);
        const descendants = this.treeControl.getDescendants(node);
        const descAllSelected = descendants.length > 0 && descendants.every((child) => {
            return this.checklistSelection.isSelected(child);
        });
        if (nodeSelected && !descAllSelected) {
            this.checklistSelection.deselect(node);
        } else if (!nodeSelected && descAllSelected) {
            this.checklistSelection.select(node);
        }
    }

    /* Get the parent node of a node */
    getParentNode(node: LicenseTreeFlatNode): LicenseTreeFlatNode | null {
        const currentLevel = this.getLevel(node);

        if (currentLevel < 1) {
            return null;
        }

        const startIndex = this.treeControl.dataNodes.indexOf(node) - 1;

        for (let i = startIndex; i >= 0; i--) {
            const currentNode = this.treeControl.dataNodes[i];

            if (this.getLevel(currentNode) < currentLevel) {
                return currentNode;
            }
        }
        return null;
    }

    /** Select the category so we can insert the new item. */
    addNewItem(node: LicenseTreeFlatNode) {
        const parentNode = this.flatNodeMap.get(node);
        this._database.insertItem(parentNode!, '');
        this.treeControl.expand(node);
    }

    /** Save the node to database */
    saveNode(node: LicenseTreeFlatNode, itemValue: string) {
        const nestedNode = this.flatNodeMap.get(node);
        this._database.updateItem(nestedNode!, itemValue);
    }

    public deleteItem(node: LicenseTreeFlatNode): void {

        // Get the parent node of the selected child node
        if (this.treeParentData.hasOwnProperty(node.name)) {
            this.treeParentData[node.name] = true;

            // It mean user selected parent node so we need to select all the childred node
            for (const child in this.treeData[node.name]) {
                if (this.treeData[node.name].hasOwnProperty(child)) {
                    this.treeData[node.name][child] = true;
                } }

            this.treeControl.expand(node);
            return;
        }
        const parentNode = this.getParentNode(node);
        // Map from flat node to nested node.
        this.treeData[parentNode.name][node.name] = true;
        this.treeControl.expand(node);

    }

    undoDelteItemd(node: LicenseTreeFlatNode): void {

        // Get the parent node of the selected child node
        if (this.treeParentData.hasOwnProperty(node.name)) {
            this.treeParentData[node.name] = false;

            // undo parent children selection
            for (const child in this.treeData[node.name]) {
                if (this.treeData[node.name].hasOwnProperty(child)) {
                this.treeData[node.name][child] = false;
                }
            }
            this.treeControl.expand(node);
            return;
        }
        const parentNode = this.getParentNode(node);
        // Map from flat node to nested node.
        this.treeData[parentNode.name][node.name] = false;
        this.treeControl.expand(node);

    }

    public isObjectEmpty(obj) {
        for (const prop in obj) {
            if (obj.hasOwnProperty(prop)) {
                return false;
            }
        }
        return true;
    }

    public checkNode(node: LicenseTreeFlatNode) {

        // check if dictionary is empty or not
        if (this.isObjectEmpty(this.treeData)) {
            return false;
        }
        // check is parent not child
        if (this.treeParentData.hasOwnProperty(node.name)) {
            return this.treeParentData[node.name];
        }

        const parentNode = this.getParentNode(node);
        return this.treeData[parentNode.name][node.name];
    }

    ngOnInit() {
        const observables = [];
        observables.push(this.wlService.getData()
            .pipe(
                map(
                    (data) => {
                        this.currentLicenseText = data;
                        this.initialLicenseText = data;
                        this.loading = false;
                    }),
                catchError(() => {
                    this.handleMissingLicense();
                    return Observable.of(null);
                })
            ));

        if (this.licenseEngine) {
            this.firstFormGroup = this.formBuilder.group({
                link: ['', Validators.required],
                branch: ['']
            });
            this.secondFormGroup = this.formBuilder.group({
                secondCtrl: ['', Validators.required]
            });

            // when license engine is used and is running, get licenses from there
            observables.push(this.leService.getAllLicenses().pipe(
                map(
                    (licenses) => {
                        this.options = licenses;
                    }),
                catchError(() => {
                    return Observable.of(null);
                })
            ));
        } else {
            // else, get licenses from the local files
            observables.push(
                this.wlService.loadLocalLicenses()
                    .pipe(
                        catchError((e) => {
                            this.handleError(e);
                            return Observable.of(null);
                        })
                    ));
            this.options = this.wlService.getLicenseNames();
        }

        forkJoin(observables).subscribe(() => {
            this.loading = false;
            this.selectedOptions = this.options;
        });
    }

    onSubmitTemplateBased() {
        const toscaElements = this.sharedData.path.split('/');
        const toscaElementID = toscaElements[toscaElements.length - 1];
        this.leService.createSoftware(toscaElementID, this.firstFormGroup.value['link'], this.firstFormGroup.value['branch']);

        this.loadingbar = true;
        this.firstFormGroup.disable();

        this.leService.deleteId().subscribe((success) => {
            if (success) {
                this.postSoftware();
            } else {
                this.failed();
            }
        }, () => {
            this.failed();
        });
    }

    postSoftware() {
        this.leService.postSoftware().subscribe(
            (success) => {
                if (success) {
                    this.leService.poll().subscribe(undefined, undefined, () => {
                        this.checkLicenseData();
                    });
                } else {
                    this.failed();
                }
            }, () => {
                this.failed();
            });
    }

    checkLicenseData() {
        if (this.leService.isFinished()) {
            const toscaElements = this.sharedData.path.split('/');
            const toscaElementID = toscaElements[toscaElements.length - 1];
            this.licenseID = toscaElementID;
            let tempDict = {};
            this.leService.getLicensewithFiles(toscaElementID).subscribe((data) => {
                // separating other license
                if (data.hasOwnProperty('UNKNOWN LICENSE')) {
                    this.unknowLicense = data['UNKNOWN LICENSE'];
                    delete data['UNKNOWN LICENSE'];
                } else { this.unknowLicense = []; }
                if (data.hasOwnProperty('NULL LICENSE')) {

                    this.nullLicense = data['NULL LICENSE'];
                    delete data['NULL LICENSE'];
                } else {
                    this.nullLicense = [];
                }
                for (const entry in data) {
                    if (data.hasOwnProperty(entry)) {
                    const tempList = [];
                    for (const index in data[entry]) {

                        if (data[entry].hasOwnProperty(index)) {
                        tempDict[data[entry][index]] = false;
                        tempList.push(data[entry][index]);
                    } }
                    this.treeData[entry] = tempDict;
                    this.treeParentData[entry] = false;
                    tempDict = {};
                    this._database.TREE_DATA[entry] = tempList;
                    const mapKey = entry;
                    const mapValue = data[entry];
                    this.foundLicenseswithFiles.set(mapKey, mapValue);
                }}

                this._database.initialize();

                this.foundLicenses = data;
            });
            this.loadingbar = false;
            this.firstFormGroup.enable();
            this.stepper.selected.completed = true;
            this.stepper.next();
        } else {
            this.failed();
        }
    }

    deleteFromBackend() {
        const filenames = [];
        for (const key in this.treeData) {
            if (this.treeData.hasOwnProperty(key)) {
            for (const val in this.treeData[key]) {
                if (this.treeData[key][val]) {

                    filenames.push((val));
                }
            }
        } }
        if (filenames.length === 0) {
            return;
        }
        this.leService.excludeFile(filenames).subscribe();
    }

    onSubmitCheckCompatibility() {
        this.loadingbar = true;
        this.compatibleLicenses = [];
        this.leService.getCompatibleLicenses().subscribe((licenses) => {
                if (licenses.length > 0) {
                    this.getLicenseInformation(licenses);
                } else {
                    this.compatibleLicenses = null;
                }
            },
            (error) => {
                this.handleError(error);
            });
    }

    getLicenseInformation(cLicenses: string[]) {
        const observables = [];
        for (const cLicense of cLicenses) {
            observables.push(this.leService.getLicenseInformation(cLicense).pipe(
                map(
                    (license) => {
                        this.compatibleLicenses.push(license);
                    })
            ));
        }
        forkJoin(observables).subscribe(() => {
            this.stepper.selected.completed = true;
            this.stepper.next();
        });
    }

    saveConfirm() {
        if (this.licenseEngine && this.currentLicenseText === this.selectedLicenseText) {
            this.confirmSaveModalRef = this.modalService.show(this.confirmSaveModal);
        } else {
            this.saveLicenseFile();
        }
    }

    saveLicenseFile() {
        this.wlService.save(this.currentLicenseText).subscribe(
            () => {
                this.handleSave();
            },
            (error) => {
                this.handleError(error);
            }
        );
        this.initialLicenseText = this.currentLicenseText;
        if (this.licenseEngine) {
            this.confirmDownloadModalRef = this.modalService.show(this.confirmDownloadModal);
        }
    }

    downloadFile() {
        const blob = new Blob([this.currentLicenseText], { type: 'text/plain;charset=utf-8' });
        saveAs(blob, 'LICENSE');
    }

    dropdownAction(item: string) {
        this.licenseType = item;
        this.showForm = false;
        if (!this.licenseEngine) {
            this.currentLicenseText = this.wlService.getLicenseTextLocal(item);
            this.selectedLicenseText = this.currentLicenseText;
        } else {
            this.loadingLicense = true;
            this.leService.getLicenseTextEngine(item)
                .subscribe(
                    (response) => {
                        this.loadingLicense = false;
                        this.currentLicenseText = response;
                        this.selectedLicenseText = this.currentLicenseText;
                    });
        }
    }

    reset() {
        if (this.checkLicensesAvailability()) {
            this.firstFormGroup.enable();
            this.showForm = true;
            this.leService.resetLicenseData();
            this.loadingbar = false;
        }
    }

    back() {
        this.loadingbar = false;
    }

    cancelEdit() {
        this.currentLicenseText = this.initialLicenseText;
        this.licenseType = '';
        this.isEditable = false;
        if (this.licenseEngine) {
            this.leService.resetLicenseData();
            this.loadingbar = false;
            this.showForm = false;
            this.firstFormGroup.enable();
        }
    }

    selectLicense(license: string) {
        const index = this.options.findIndex(item => license.toLowerCase() === item.toLowerCase());
        if (index >= 0) {
            license = this.options[index];
            this.dropdownAction(license);
        } else {
            this.notify.error('Unknown license selected!');
        }
    }

    getStatus() {
        return this.leService.getStatus();
    }

    failed() {
        this.leService.failed();
    }

    isFailed() {
        return this.leService.isFailed();
    }

    checkLicensesAvailability(): boolean {
        if (this.licenseEngine && this.options.length === 0) {
            this.notify.error('Unable to access the License Engine!');
            return false;
        }
        return true;
    }

    search(eventTarget: EventTarget) {
        const value = (eventTarget as HTMLInputElement).value;
        this.selectedOptions = this.select(value);
    }

    select(query: string): string[] {
        const result: string[] = [];
        for (const license of this.options) {
            if (license.toLowerCase().startsWith(query.toLowerCase())) {
                result.push(license);
            }
        }
        return result;
    }

    private handleError(error: HttpErrorResponse) {
        this.loading = false;
        this.notify.error(error.message);
    }

    private handleMissingLicense() {
        this.loading = false;
        this.licenseAvailable = false;
    }

    private handleSave() {
        this.notify.success('Successfully saved LICENSE');
    }
}

