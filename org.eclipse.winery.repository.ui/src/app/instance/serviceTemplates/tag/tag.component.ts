/**
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Lukas Balzer, Nicole Keppler - initial API and implementation
 */
import { Component, OnInit, ViewChild } from '@angular/core';
import { TagService } from './tag.service';
import { TagsAPIData } from './tagsAPIData';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { isNullOrUndefined } from 'util';
import { WineryValidatorObject } from '../../../wineryValidators/wineryDuplicateValidator.directive';
import { ModalDirective } from 'ngx-bootstrap';

@Component({
    selector: 'winery-instance-tag',
    templateUrl: 'tag.component.html',
    providers: [TagService, WineryNotificationService]
})
export class TagComponent implements OnInit {
    loading = false;
    tagsData: TagsAPIData[] = [];
    newTag: TagsAPIData = new TagsAPIData();
    selectedCell: any;
    validatorObject: WineryValidatorObject;

    columns: Array<any> = [
        {title: 'id', name: 'id', sort: true},
        {title: 'name', name: 'name', sort: true},
        {title: 'value', name: 'value', sort: true},
    ];
    @ViewChild('confirmDeleteModal') confirmDeleteModal: ModalDirective;
    @ViewChild('addModal') addModal: ModalDirective;

    public constructor(private service: TagService,
                       private noteService: WineryNotificationService) {
    }

    ngOnInit(): void {
        this.getTagsData();
    }

    onCellSelected(event: TagsAPIData) {
        this.selectedCell = event;
    }

    onRemoveClick() {
        if (isNullOrUndefined(this.selectedCell)) {
            this.noteService.error('no cell selected!');
        } else {
          this.confirmDeleteModal.show();
        }
    }

    onAddClick() {
        this.validatorObject = new WineryValidatorObject(this.tagsData, 'name');
        this.newTag = new TagsAPIData();
        this.addModal.show();
    }

    getTagsData() {
        this.service.getTagsData().subscribe(
            data => this.handleTagsData(data),
            error => this.handleError(error)
        );
    }

    addNewTag() {
        this.service.postTag(this.newTag).subscribe(
            data => this.handleSuccess(data),
            error => this.handleError(error)
        );
    }

    removeConfirmed() {
        this.service.removeTagData(this.selectedCell).subscribe(
            data => this.handleRemoveSuccess(),
            error => this.handleError(error)
        );
    }

    handleSuccess(data: string) {
        this.newTag.id = data;
        this.tagsData.push(this.newTag);
        this.noteService.success('Added new Tag');
    }

    handleRemoveSuccess() {
        this.selectedCell = null;
        this.getTagsData();
        this.noteService.success('Removed Tag');
    }

    private handleTagsData(data: TagsAPIData[]) {
        this.tagsData = data;
        this.loading = false;
    }

    private handleError(error: any): void {
        this.loading = false;
        this.noteService.error('Action caused an error:\n', error);
    }
}
