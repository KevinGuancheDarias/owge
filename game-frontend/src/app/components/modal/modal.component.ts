import { Component, ElementRef, Input, OnInit } from '@angular/core';

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
export class ModalComponent implements OnInit {

    @Input()
    public closeOnOverlayClick = true;

    @Input()
    public isOpenOnLoad = false;

    @Input()
    public hasCloseButton = false;

    public visible = false;
    public visibleAnimate = false;


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
    ) { }

    public ngOnInit(): void {
        if (this.isOpenOnLoad) {
            this.show();
        }
    }

    public show(): void {
        this.visible = true;
        setTimeout(() => this.visibleAnimate = true, 100);
    }

    public hide(): void {
        this.visibleAnimate = false;
        setTimeout(() => this.visible = false, 300);
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
}
