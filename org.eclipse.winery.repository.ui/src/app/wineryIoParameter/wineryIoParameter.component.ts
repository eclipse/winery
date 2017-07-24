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
 */
import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { WineryTableColumn } from '../wineryTableModule/wineryTable.component';
import { InterfaceParameter } from '../wineryInterfaces/parameters';
import { WineryValidatorObject } from '../wineryValidators/wineryDuplicateValidator.directive';
import { YesNoEnum } from '../wineryInterfaces/enums';
import { WineryNotificationService } from '../wineryNotificationModule/wineryNotification.service';
import { NgForm } from '@angular/forms';
import { ModalDirective } from 'ngx-bootstrap';

/**
 * This component provides two tables for adding and removing input and output parameters as they are used for example
 * in the {@link InterfacesComponent}. Therefore you need to specify the arrays containing the input/output parameters
 * as an {@link InterfaceParameter} array. Additionally, there are some events which fire upon adding/removing elements.
 *
 * <label>Inputs</label>
 * <ul>
 *     <li><code>inputParameters</code> the array for the input parameters. It must be of type {@link InterfaceParameter}.
 *     </li>
 *     <li><code>outputParameters</code> the array for the output parameters. It must be of type {@link InterfaceParameter}.
 *     </li>
 * </ul>
 *
 * <label>Outputs</label>
 * <ul>
 *     <li><code>inputParameterAdded</code> fires upon adding a input parameter to the table. It also contains the
 *     added element which is of type {@link InterfaceParameter}..
 *     </li>
 *     <li><code>outputParameterAdded</code> fires upon adding a output parameter to the table. It also contains the
 *     added element which is of type {@link InterfaceParameter}..
 *     </li>
 *     <li><code>inputParameterRemoved</code> fires upon removing a input parameter from the table. It also contains the
 *     removed element which is of type {@link InterfaceParameter}.
 *     </li>
 *     <li><code>outputParameterRemoved</code> fires upon removing a output parameter from the table. It also contains the
 *     removed element which is of type {@link InterfaceParameter}.
 *     </li>
 * </ul>
 *
 * @example <caption>Minimalistic example:</caption>
 * ```
 * <winery-io-parameter [inputParameters]="newPlan.inputParameters.inputParameter"
 *                      [outputParameters]="newPlan.outputParameters.outputParameter">
 * </winery-io-parameter>
 * ```
 */
@Component({
    selector: 'winery-io-parameter',
    templateUrl: './wineryIoParameter.component.html'
})
export class WineryIoParameterComponent {

    @Input() inputParameters: InterfaceParameter[];
    @Input() outputParameters: InterfaceParameter[];

    @Output() inputParameterAdded = new EventEmitter<InterfaceParameter>();
    @Output() outputParameterAdded = new EventEmitter<InterfaceParameter>();
    @Output() inputParameterRemoved = new EventEmitter<InterfaceParameter>();
    @Output() outputParameterRemoved = new EventEmitter<InterfaceParameter>();

    @ViewChild('addIntParametersModal') addIntParametersModal: ModalDirective;
    @ViewChild('removeElementModal') removeElementModal: ModalDirective;
    @ViewChild('parameterForm') parameterForm: NgForm;

    selectedInputParameter: InterfaceParameter;
    selectedOutputParameter: InterfaceParameter;
    columns: Array<WineryTableColumn> = [
        {title: 'Name', name: 'name', sort: true},
        {title: 'Type', name: 'type', sort: true},
        {title: 'Required', name: 'required', sort: false}
    ];

    modalTitle: string;
    elementToRemove: string;
    validatorObject: WineryValidatorObject;

    constructor(private notify: WineryNotificationService) {

    }

    // region ########## Input Parameters ##########
    addInputParam() {
        this.modalTitle = 'Input Parameter';
        this.validatorObject = new WineryValidatorObject(this.inputParameters, 'name');
        this.parameterForm.reset();
        this.addIntParametersModal.show();
    }

    onAddInputParam(name: string, type: string, required: boolean) {
        const item = new InterfaceParameter(name, type, required ? YesNoEnum.YES : YesNoEnum.NO);
        this.inputParameters.push(item);
        this.inputParameterAdded.emit(item);
    }

    onInputParameterSelected(selectedInput: InterfaceParameter) {
        this.selectedInputParameter = selectedInput;
    }

    removeInputParameter() {
        this.modalTitle = 'Remove Input Parameter';
        this.elementToRemove = this.selectedInputParameter.name;
        this.removeElementModal.show();
    }

    onRemoveInputParameter() {
        this.inputParameters.splice(this.inputParameters.indexOf(this.selectedInputParameter));
        this.inputParameterRemoved.emit(this.selectedInputParameter);
        this.selectedInputParameter = null;
    }

    // endregion

    // region ########## Output Parameters ##########
    addOutputParam() {
        this.modalTitle = 'Output Parameter';
        this.validatorObject = new WineryValidatorObject(this.outputParameters, 'name');
        this.parameterForm.reset();
        this.addIntParametersModal.show();
    }

    onAddOutputParam(name: string, type: string, required: boolean) {
        const item = new InterfaceParameter(name, type, required ? YesNoEnum.YES : YesNoEnum.NO);
        this.outputParameters.push(item);
        this.outputParameterAdded.emit(item);
    }

    onOutputParameterSelected(selectedOutput: InterfaceParameter) {
        this.selectedOutputParameter = selectedOutput;
    }

    removeOutputParameter() {
        this.modalTitle = 'Remove Output Parameter';
        this.elementToRemove = this.selectedOutputParameter.name;
        this.removeElementModal.show();
    }

    onRemoveOutputParameter() {
        this.outputParameters.splice(this.outputParameters.indexOf(this.selectedOutputParameter));
        this.outputParameterRemoved.emit(this.selectedOutputParameter);
        this.selectedOutputParameter = null;
    }

    // endregion

    onRemoveElement() {
        switch (this.modalTitle) {
            case 'Remove Input Parameter':
                this.onRemoveInputParameter();
                break;
            case 'Remove Output Parameter':
                this.onRemoveOutputParameter();
                break;
            default:
                this.notify.error('Couldn\'t remove element!');
        }
    }

}
