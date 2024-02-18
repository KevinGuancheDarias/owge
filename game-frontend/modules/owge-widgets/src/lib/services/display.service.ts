import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { BackendError } from '@owge/types/core';
import { take } from 'rxjs/operators';

/**
 * Displays something in an alert(), confirm(), etc, used because in the future those actions may be styled
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class DisplayService {

    public constructor(private _translateService: TranslateService) { }

    /**
     * Displays a backend error
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  err
     * @param interpolationArguments Props to allow usage in the view of the translation,
     *          for example itemName, would be accesible with {{itenName}}
     * @returns
     */
    public async showBackendError(err: BackendError, interpolationArguments?: Object): Promise<void> {
        this.alert(await this._findErrorTranslation(err, interpolationArguments));
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  message
     * @returns
     */
    public async alert(message: string): Promise<void> {
        alert(message);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  message
     * @returns
     */
    public async error(message: string): Promise<void> {
        alert(message);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  message
     * @returns
     */
    public async confirm(message: string): Promise<boolean> {
        return confirm(message);
    }

    private _findErrorTranslation(err: BackendError, interpolationArguments?: Object): Promise<string> {
        const targetProp = err.message.startsWith('I18N')
            ? err.message
            : 'generic';
        return this._translateService.get(`EXCEPTIONS.${err.reporterAsString.replace(/\./g, '_')}.${targetProp}`, interpolationArguments)
            .pipe(take(1)).toPromise();
    }
}
