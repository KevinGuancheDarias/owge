import {Directive, ElementRef, OnDestroy, Renderer2} from '@angular/core';
import {ObsService} from '../services/obs.service';
import {Subscription} from 'rxjs';

@Directive({
    selector: '[owgeCoreSecureItem]'
})
export class SecureItemDirective implements OnDestroy{
    #subscription: Subscription;

    constructor(renderer: Renderer2, hostElement: ElementRef, obsService: ObsService) {
        this.#subscription = obsService.isStreaming.subscribe(isStreaming => {
            if(isStreaming) {
                renderer?.addClass(hostElement.nativeElement, 'obs-is-streaming');
            } else {
                renderer?.removeClass(hostElement.nativeElement, 'obs-is-streaming');
            }
        });
    }

    ngOnDestroy(): void {
        this.#subscription.unsubscribe();
    }
}
