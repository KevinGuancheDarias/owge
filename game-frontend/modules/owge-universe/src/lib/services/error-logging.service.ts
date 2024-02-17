import {Injectable} from '@angular/core';
import {UniverseGameService} from './universe-game.service';
import {stringify} from 'flatted';

@Injectable({
    providedIn: 'root'
})
export class ErrorLoggingService {
    private readonly originalWarn = console.warn.bind(console);

    constructor(private universeGameService: UniverseGameService) {
        window.onerror = (msg, url, lineNo, columnNo, error) => {
            console.log('ups',msg,lineNo, error);
        };
        ['warn','error'].forEach(level => this.intercept(level));
    }

    private intercept(method: string): void {
            // eslint-disable-next-line no-console
            const original = console[method].bind(console);
            // eslint-disable-next-line no-console
            console[method] = (...args) => {
                // eslint-disable-next-line no-console
                this.doReport(method, args).then(() => console.debug('Logged data ',method, args));
                original(...args);
            };
    }

    private async doReport(method: string, args: unknown[]): Promise<void> {
        try {
            const content = stringify(args);
            if(content.length > 100) {
                await this.universeGameService.requestWithAutorizationToContext(
                    'game', 'post', `track-browser/${method}`, content
                ).toPromise();
            } else {
                this.originalWarn('Not logging due to insufficient information',content);
            }
        }catch (e) {
            this.originalWarn('unable to track browser',e );
        }
    }
}
