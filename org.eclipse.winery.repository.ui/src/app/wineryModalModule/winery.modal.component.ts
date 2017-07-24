/**
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Tino Stadelmaier - initial API and implementation
 */
import { AfterContentInit, AfterViewInit, Component, ContentChild, HostBinding, Input } from '@angular/core';
import { isNullOrUndefined } from 'util';
import { WineryModalFooterComponent } from './winery.modal.footer.component';
import { WineryModalHeaderComponent } from './winery.modal.header.component';
import { ModalDirective } from 'ngx-bootstrap';

const SMALL = 'sm';
const LARGE = 'lg';

/**
 * This component provides a generic modal component for any kind of pop-ups.
 * To use it, the {@link WineryModalModule} must be imported in the corresponding module.
 *
 * In order to use this component, see the following example, note that the <code>modalRef</code> must be set.
 * For further information, see the sub-components {@link WineryModalHeaderComponent}, {@link WineryModalBodyComponent},
 * and {@link WineryModalFooterComponent}.
 *
 * <label>Inputs</label>
 * <ul>
 *     <li><code>modalRef</code> The modalRef must be set, otherwise the component will not work!
 *     </li>
 *     <li><code>size</code>
 *     </li>
 *     <li><code>keyboard</code>
 *     </li>
 *     <li><code>backdrop</code>
 *     </li>
 * </ul>
 *
 * @example <caption>Short Example</caption>
 * ```html
 * <winery-modal bsModal #confirmDeleteModal="bs-modal" [modalRef]="confirmDeleteModal">
 *     <winery-modal-header [title]="'Delete Property'">
 *     </winery-modal-header>
 *     <winery-modal-body>
 *         <p *ngIf="elementToRemove != null">
 *         Do you want to delete the Element
 *             <span style="font-weight:bold;">{{ elementToRemove.key }}</span>?
 *         </p>
 *     </winery-modal-body>
 *     <winery-modal-footer (onOk)="removeConfirmed();"
 *                          [closeButtonLabel]="'Cancel'"
 *                          [okButtonLabel]="'Delete'">
 *     </winery-modal-footer>
 * </winery-modal>
 * ```
 */
@Component({
    selector: 'winery-modal',
    templateUrl: 'winery.modal.component.html',
})
export class WineryModalComponent implements AfterViewInit, AfterContentInit {

    @Input() modalRef: ModalDirective;
    @Input() size: string;
    @Input() keyboard = true;
    @Input() backdrop = true;

    @HostBinding('class') hostClass = 'modal fade';
    @HostBinding('attr.role') hostRole = 'dialog';
    @HostBinding('tabindex') hostTabIndex = '-1';

    @ContentChild(WineryModalHeaderComponent) headerContent: WineryModalHeaderComponent;
    @ContentChild(WineryModalFooterComponent) footerContent: WineryModalFooterComponent;

    private overrideSize: string = null;
    private cssClass = '';

    ngAfterContentInit(): void {
        if (!isNullOrUndefined(this.headerContent)) {
            this.headerContent.modalRef = this.modalRef;
        }
        if (!isNullOrUndefined(this.footerContent)) {
            this.footerContent.modalRef = this.modalRef;
        }
    }

    ngAfterViewInit(): void {
        if (!this.backdrop) {
            this.modalRef.config.backdrop = 'static';
        } else {
            this.modalRef.config.backdrop = this.backdrop;
        }

        this.modalRef.config.keyboard = this.keyboard;

        if (ModalSize.validSize(this.size)) {
            this.overrideSize = this.size;
        }
    }

    getCssClasses(): string {
        const classes: string[] = [];

        if (this.isSmall()) {
            classes.push('modal-sm');
        }

        if (this.isLarge()) {
            classes.push('modal-lg');
        }

        if (this.cssClass !== '') {
            classes.push(this.cssClass);
        }

        return classes.join(' ');
    }

    private isSmall() {
        return this.overrideSize !== LARGE
            && this.size === SMALL
            || this.overrideSize === SMALL;
    }

    private isLarge() {
        return this.overrideSize !== SMALL
            && this.size === LARGE
            || this.overrideSize === LARGE;
    }
}

/**
 * This class is used to determine the modal's size
 */
export class ModalSize {
    static validSize(size: string) {
        return size && (size === SMALL || size === LARGE);
    }

}
