import {Injectable} from '@angular/core';
import OBSWebSocket from 'obs-websocket-js';
import {LoggerHelper} from '../helpers/logger.helper';
import {BehaviorSubject, Observable, Subject} from 'rxjs';

@Injectable()
export class ObsService {

    get isStreaming(): Observable<boolean> {
        return this.#isStreaming.asObservable();
    }

    #isStreaming: Subject<boolean> = new BehaviorSubject(false);
    private websocket: OBSWebSocket;
    private log: LoggerHelper = new LoggerHelper('ObsService');

    constructor() {
        (window as any).configureOwgeObs = (password: string) => {
            localStorage.setItem('owge_obs_password', password);
            this.doConnectObs(password).then(() => this.log.debug('Connected to obs websocket after configuration'));
        };
        const currentPassword = localStorage.getItem('owge_obs_password');
        if(currentPassword) {
            this.doConnectObs(currentPassword).then(() => this.log.debug('Connected to obs websocket because it\'s configured to do so '));
        }
    }

    /**
     *
     * @see https://github.com/obs-websocket-community-projects/obs-websocket-js/discussions/336
     */
    private async doConnectObs(password: string): Promise<void> {
        let invocationTimes = 0;
        if(this.websocket) {
            await this.websocket.disconnect();
            this.websocket.removeAllListeners();
        }
        this.websocket = new OBSWebSocket();
        await this.websocket.connect('ws://127.0.0.1:4455', password);

        this.websocket.on('RecordStateChanged', event => {
            invocationTimes++;
            if(invocationTimes % 2 === 0) { // Due to a bug in obs websocket workaround the dupplicated events
                this.log.debug(`Changed streaming status to ${event.outputActive}`);
                this.setStreamingState(event.outputActive);
            }
        });
        await this.websocket.call('GetRecordStatus').then(recordStatus => {
            this.log.debug(`Initial streaming status is ${recordStatus.outputActive}`);
            this.setStreamingState(recordStatus.outputActive);
        });
    }

    private setStreamingState(newVal: boolean) {
        if(newVal === false) {
            setTimeout(() => this.#isStreaming.next(false), 300);
        } else {
            this.#isStreaming.next(newVal);
        }
    }
}
