import { Component, ElementRef, Input, OnChanges, OnInit, ViewChild } from '@angular/core';
import { ReplaySubject , Observable } from 'rxjs';

import { AbstractModalComponent } from '../../interfaces/abstract-modal-component';

/**
 * Displays a modal with custom content
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
@Component({
    selector: 'owge-core-modal',
    templateUrl: './modal.component.html',
    styleUrls: ['./modal.component.less']
})
export class ModalComponent extends AbstractModalComponent implements OnInit, OnChanges {
    @Input() extraClasses: Record<string, unknown>;

    @ViewChild('modalRoot', { static: true }) private _modalRoot: ElementRef;

    containerClasses: Record<string, unknown> = {};
    visible = false;
    visibleAnimate = false;

    private _status: ReplaySubject<boolean> = new ReplaySubject(1);

    /**
     * Returns if the modal is visible or not, true for visible
     *
     * @since 0.7.0
     */
    get status(): Observable<boolean> {
        return this._status.asObservable();
    }

    /**
     * If true will close the modal when the user clicks outside the modal <br>
     * Defaults to <b>true</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private _closeOnContainerClicked = true;

    constructor(
        private _ref: ElementRef
    ) {
        super();
    }

    ngOnInit(): void {
        this.addInAnimateClass();
        if (this.isOpenOnLoad) {
            this.show();
        }
    }

    ngOnChanges(): void {
       this.containerClasses = {...this.containerClasses, ...this.extraClasses};
    }

    show(): void {
        this._moveModalToBody();
        this.visible = true;
        this._status.next(true);
        setTimeout(() => this.visibleAnimate = true, 100);
    }

    hide(): void {
        this.visibleAnimate = false;
        this._status.next(false);
        setTimeout(() => {
            this.visible = false;
            this._applyHotfix();
        }, 300);
    }

    onContainerClicked(event: MouseEvent): void {
        if (this.closeOnOverlayClick && this._closeOnContainerClicked && (event.target as HTMLElement).classList.contains('modal')) {
            this.hide();
        }
    }

    setCloseOnContainerClicked(value: boolean): void {
        this._closeOnContainerClicked = value;
    }

    getRef(): ElementRef {
        return this._ref;
    }


    /**
     * Adds the modal to the body (avoids problems with modal positioning)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private _moveModalToBody(): void {
        document.body.appendChild(this._modalRoot.nativeElement);
    }

    /**
     * For unknown reasons, databinding to display property is not working in some cases
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @see https://trello.com/c/u2yS6yGH
     */
    private _applyHotfix(): void {
        this._modalRoot.nativeElement.style.display = 'none';
    }

    private addInAnimateClass(): void {
        this.containerClasses.in = this.visibleAnimate;
    }
}
