/**
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Lukas Balzer - initial API and implementation
 */
import { Component, OnInit } from '@angular/core';
import { VisualAppearanceService } from './visualAppearance.service';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { isNullOrUndefined } from 'util';
import { RelationshipTypesVisualsApiData } from './relationshipTypesVisualsApiData';
import { NodeTypesVisualsApiData } from './nodeTypesVisualsApiData';

@Component({
    templateUrl: 'visualAppearance.component.html',
    styleUrls: [
        'visualAppearance.component.css'
    ],
    providers: [VisualAppearanceService]
})
export class VisualAppearanceComponent implements OnInit {

    relationshipData: RelationshipTypesVisualsApiData;
    nodeTypeData: NodeTypesVisualsApiData;
    loading = true;
    img16Path: string;
    img50Path: string;
    isNodeType = true;

    constructor(private service: VisualAppearanceService,
                private notify: WineryNotificationService) {
    }

    ngOnInit() {
        this.loading = true;
        this.img16Path = this.service.getImg16x16Path();
        this.img50Path = this.service.getImg50x50Path();

        if (this.service.path.includes('relationshiptypes')) {
            this.isNodeType = false;
        }

        if (this.isNodeType) {
            this.getNodeTypeData();
        } else {
            this.getRelationshipData();
        }
    }

    /**
     * @param type the part of the arrow that should be changed<p>
     *             should be one of
     *             <ul>
     *                 <li>dash
     *                 <li>sourceArrowHead
     *                 <li>targetArrowHead
     *             </ul>
     * @param style the style of the line which should be one of the styles accepted by jsPlumb:<p>
     *              <b>for source-/targetArrowHead</b>
     *              <ul>
     *                  <li>none
     *                  <li>PlainArrow
     *                  <li>Diamond
     *              </ul><b>for dash</b>
     *              <ul>
     *                  <li>plain
     *                  <li>dotted
     *                  <li>dotted2
     *              </ul>
     */
    selectArrowItem(type?: string, style?: string) {
        const hasType = !isNullOrUndefined(type);
        const hasStyle = !isNullOrUndefined(style);
        let dashSelected = false;
        let sourcearrowheadSelected = false;
        let targetarrowheadSelected = false;
        if (hasType && type === 'dash') {
            this.relationshipData.dash = hasStyle ? style : this.relationshipData.dash;
            dashSelected = !this.relationshipData.boolData.dashSelected;
        } else if (hasType && type === 'sourceArrowHead') {
            this.relationshipData.sourceArrowHead = hasStyle ? style : this.relationshipData.sourceArrowHead;
            sourcearrowheadSelected = !this.relationshipData.boolData.sourceArrowHeadSelected;
        } else if (hasType && type === 'targetArrowHead') {
            this.relationshipData.targetArrowHead = hasStyle ? style : this.relationshipData.targetArrowHead;
            targetarrowheadSelected = !this.relationshipData.boolData.targetArrowHeadSelected;
        }
        this.relationshipData.boolData.dashSelected = dashSelected;
        this.relationshipData.boolData.sourceArrowHeadSelected = sourcearrowheadSelected;
        this.relationshipData.boolData.targetArrowHeadSelected = targetarrowheadSelected;
    }

    saveToServer() {
        if (this.isNodeType) {
            this.service.saveVisuals(new NodeTypesVisualsApiData(this.nodeTypeData)).subscribe(
                data => this.handleResponse(data),
                error => this.handleError(error)
            );
        } else {
            this.service.saveVisuals(new RelationshipTypesVisualsApiData(this.relationshipData, false)).subscribe(
                data => this.handleResponse(data),
                error => this.handleError(error)
            );
        }
    }

    getRelationshipData() {
        this.service.getData().subscribe(
            data => this.handleRelationshipData(data),
            error => this.handleError(error)
        );
    }

    getNodeTypeData() {
        this.service.getData().subscribe(
            data => this.handleColorData(data),
            error => this.handleError(error)
        );
    }

    handleColorData(data: any) {
        this.nodeTypeData = new NodeTypesVisualsApiData(data);
        this.loading = false;
    }

    handleRelationshipData(data: any) {
        this.relationshipData = new RelationshipTypesVisualsApiData(data, true);
        this.loading = false;
    }

    onUploadSuccess() {
        this.loading = true;
        if (this.isNodeType) {
            this.getNodeTypeData();
        } else {
            this.getRelationshipData();
        }
    }

    public get colorLocal() {
        return this.relationshipData.color;
    }

    public set colorLocal(color: string) {
        this.relationshipData.color = color;
    }

    public get borderColorLocal() {
        return this.nodeTypeData.color;
    }

    public set borderColorLocal(color: string) {
        this.nodeTypeData.color = color;
    }

    public get hoverColorLocal() {
        return this.relationshipData.hovercolor;
    }

    public set hoverColorLocal(color: string) {
        this.relationshipData.hovercolor = color;
    }

    private handleResponse(response: any) {
        this.loading = false;
        this.notify.success('Successfully saved visual data!');
    }

    private handleError(error: any): void {
        this.loading = false;
        this.notify.error(error);
    }

}
