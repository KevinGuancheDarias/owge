import {OnDestroy, Pipe, PipeTransform} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {Planet} from '@owge/universe';
import {ObsService} from '../services/obs.service';
import {map} from 'rxjs/operators';
import {PlanetDisplayNamePipe} from './planet-display-name.pipe';

@Pipe({
    name: 'securePlanetEntryPipe',
    pure: false
})
export class SecurePlanetEntryPipePipe extends PlanetDisplayNamePipe implements PipeTransform, OnDestroy {

    _subscription: Subscription;
    _inputArg: Planet;
    _lastValue: string;
    _isStreaming = false;

    constructor(obsService: ObsService) {
        super();
         this._subscription = obsService.isStreaming.subscribe(currentStreamingState => {
             this._isStreaming = currentStreamingState;
             this.recomputeValue();
         });
    }

    transform(value: Planet): string {
        this._inputArg = value;
        if((this._inputArg !== value) || this._lastValue === undefined) {
            this.recomputeValue();
        }
        return this._lastValue;
    }

    ngOnDestroy(): void {
        this._subscription.unsubscribe();
    }

    private recomputeValue() {
        this._lastValue = this._isStreaming ? 'SECRET' : (this._inputArg && super.transform(this._inputArg));
    }
}
