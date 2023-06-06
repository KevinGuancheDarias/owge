import { Directive, Output, EventEmitter, HostListener, Renderer2, ElementRef, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { WebsocketService } from '../services/websocket.service';

/**
 * Runs a dom event handler only if connected to websocket
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Directive({
    selector: '[owgeUniverseOnClickIfWsConnected]'
})
export class OnClickIfWsConnectedDirective {

    private static readonly _CSS_CLASS = 'disabled-for-offline';

    private _isConnected = false;
    private _i18nErrorString: string;

    @Output() clickAndConnected: EventEmitter<Event> = new EventEmitter();

    public constructor(
        renderer: Renderer2,
        elementRef: ElementRef,
        websocketService: WebsocketService,
        translateService: TranslateService
    ) {
        websocketService.isConnected.subscribe(val => {
            this._isConnected = val;
            if (val) {
                renderer.removeClass(elementRef.nativeElement, OnClickIfWsConnectedDirective._CSS_CLASS);
            } else {
                renderer.addClass(elementRef.nativeElement, OnClickIfWsConnectedDirective._CSS_CLASS);
            }
        });
        translateService.get('APP.NOT_CONNECTED_CLICK_ERROR').subscribe(val => this._i18nErrorString = val);
    }

    @HostListener('click', ['$event'])
    onClick(e) {
        e.preventDefault();
        e.stopPropagation();
        if (this._isConnected) {
            this.clickAndConnected.next(e);
        } else {
            alert(this._i18nErrorString);
        }
    }
}
