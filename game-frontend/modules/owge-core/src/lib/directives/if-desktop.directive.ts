import { Directive, EventEmitter, Input, OnDestroy, OnInit, Output, TemplateRef, ViewContainerRef } from '@angular/core';
import { Subscription } from 'rxjs';
import { ScreenDimensionsService } from '../services/screen-dimensions.service';

/**
 * ngIf like directive that applies when in desktop <br>
 *
 * <b>IMPORTANT: </b> When wanting to use the event listener, won't work with asterisk syntaxs, see the "see of this jsdoc" <br>
 *
 * Inputs: <br>
 * <ul>
 * <li>minWidth: is the width to which the directive should  consider we are in desktop</li>
 * <li>negate: When true, the minWidth will act as a maxWidth, so can be used to check if we are in <b>mobile</b></li>
 * </ul> <br>
 * Outputs: <br>
 * <ul>
 * <li>changed: When changed will emit, and the emitted value will be true when the child view should be displayed<li>
 * </ul>
 *
 * @see https://stackoverflow.com/a/49019887/1922558
 * @see https://juristr.com/blog/2018/02/angular-permission-directive/
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Directive({
    selector: '[owgeCoreIfDesktop]'
})
export class OwgeCoreIfDesktopDirective implements OnInit, OnDestroy {
    @Input() public minWidth = 767;
    @Input() public negate = false;
    @Output() public changed: EventEmitter<boolean> = new EventEmitter();

    private _sdsIdentifier: string;
    private _subscription: Subscription;

    public constructor(
        private _templateRef: TemplateRef<any>,
        private _viewContainer: ViewContainerRef,
        private _screenDimensionsService: ScreenDimensionsService
    ) {
        this._sdsIdentifier = this._screenDimensionsService.generateIdentifier(this);
    }

    /**
     *
     * @see https://jsfiddle.net/KevinGuancheDarias/om3c0hd2/5/
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    public ngOnInit(): void {
        this._subscription = this._screenDimensionsService.hasMinWidth(this.minWidth, this._sdsIdentifier).subscribe(val => {
            const afterNegation = !!(val as any - (this.negate as any));
            if (afterNegation) {
                this._viewContainer.createEmbeddedView(this._templateRef);
            } else {
                this._viewContainer.clear();
            }
            this.changed.emit(afterNegation);
        });
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    public ngOnDestroy(): void {
        this._subscription.unsubscribe();
        this._screenDimensionsService.removeHandler(this._sdsIdentifier);
    }
}
