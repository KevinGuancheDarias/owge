import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { ReplaySubject } from 'rxjs/ReplaySubject';
import { Observable } from 'rxjs/Observable';

import { AbstractModalComponent } from '../../interfaces/abstract-modal-component';

/**
 * Displays a modal with custom content
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class ModalComponent
 */
@Component({
    selector: 'app-modal',
    templateUrl: './modal.component.html',
    styleUrls: ['./modal.component.less']
})
export class ModalComponent extends AbstractModalComponent implements OnInit {

    public visible = false;
    public visibleAnimate = false;

    /**
     * Returns if the modal is visible or not, true for visible
     *
     * @since 0.7.0
     * @readonly
     * @type {Observable<boolean>}
     * @memberof ModalComponent
     */
    public get status(): Observable<boolean> {
        return this._status.asObservable();
    }

    @ViewChild('modalRoot') private _modalRoot: ElementRef;
    private _status: ReplaySubject<boolean> = new ReplaySubject(1);

    /**
     * If true will close the modal when the user clicks outside the modal <br>
     * Defaults to <b>true</b>
     *
     * @memberof ModalComponent
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private _closeOnContainerClicked = true;

    public constructor(
        private _ref: ElementRef
    ) {
        super();
    }

    public ngOnInit(): void {
        if (this.isOpenOnLoad) {
            this.show();
        }
    }

    public show(): void {
        this._moveModalToBody();
        this.visible = true;
        this._status.next(true);
        setTimeout(() => this.visibleAnimate = true, 100);
    }

    public hide(): void {
        this.visibleAnimate = false;
        this._status.next(false);
        setTimeout(() => {
            this.visible = false;
            this._applyHotfix();
        }, 300);
    }

    public onContainerClicked(event: MouseEvent): void {
        if (this.closeOnOverlayClick && this._closeOnContainerClicked && (<HTMLElement>event.target).classList.contains('modal')) {
            this.hide();
        }
    }

    public setCloseOnContainerClicked(value: boolean): void {
        this._closeOnContainerClicked = value;
    }

    public getRef(): ElementRef {
        return this._ref;
    }


    /**
     * Adds the modal to the body (avoids problems with modal positioning)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @private
     * @memberof ModalComponent
     */
    private _moveModalToBody(): void {
        document.body.appendChild(this._modalRoot.nativeElement);
    }

    /**
     * For unknown reasons, databinding to display property is not working in some cases
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @private
     * @memberof ModalComponent
     * @see https://trello.com/c/u2yS6yGH
     */
    private _applyHotfix(): void {
        this._modalRoot.nativeElement.style.display = 'none';
    }
}
