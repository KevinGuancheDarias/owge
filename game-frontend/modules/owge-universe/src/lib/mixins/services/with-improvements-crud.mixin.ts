import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, empty } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { ProgrammingError, LoggerHelper } from '@owge/core';
import { BackendError, Improvement, ImprovementUnitType } from '@owge/types/core';
import { DisplayService } from '@owge/widgets';

import { UniverseGameService } from '../../services/universe-game.service';
import { CrudConfig } from '@owge/types/universe';

/**
 * Adds support for the improvements to the specified service <br>
 * <b>NOTICE:</b> It's important to define _crudConfig , _displayService and _universeGameService in the class applying this mixing
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @template K Type for id of the entity
 */
export class WithImprovementsCrudMixin<K> {
    protected _universeGameService: UniverseGameService;
    protected _crudConfig: CrudConfig;
    protected _displayService: DisplayService;
    protected _translateService: TranslateService;

    /**
     * Finds the improvement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param id target entity
     * @returns
     */
    public findImprovement(id: K): Observable<Improvement> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'get',
            `${this._crudConfig.findOneEntityPath(id)}/improvement`,
            '',
            {
                errorHandler: (err, chain) => this._wicmHandleError(err, chain)
            }
        );
    }

    /**
     * Finds the improvements that gives bonuses to specified unit types
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  id
     * @returns
     */
    public findImprovementUnitTypes(id: K): Observable<ImprovementUnitType[]> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'get',
            `${this._crudConfig.findOneEntityPath(id)}/improvement/unitTypeImprovements`,
            '',
            {
                errorHandler: (err, chain) => this._wicmHandleError(err, chain)
            }
        );
    }

    /**
     * Saves the improvement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param id
     * @param improvement
     * @returns
     */
    public saveImprovement(id: K, improvement: Improvement): Observable<Improvement> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'put',
            `${this._crudConfig.findOneEntityPath(id)}/improvement`,
            improvement
        );
    }

    /**
     * Saves a new unit type improvement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param id
     * @param improvementUnitType
     * @returns Saved entity with an id assigned
     */
    public saveImprovementUnitType(id: K, improvementUnitType: ImprovementUnitType): Observable<ImprovementUnitType> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'post',
            `${this._crudConfig.findOneEntityPath(id)}/improvement/unitTypeImprovements`,
            improvementUnitType,
            {
                errorHandler: (err, chain) => this._wicmHandleError(err, chain, improvementUnitType)
            }
        );
    }


    /**
     * Deletes a unit type improvement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param id
     * @returns
     */
    public deleteImprovementUnitType(id: K, improvementUnitTypeId: number): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'delete',
            `${this._crudConfig.findOneEntityPath(id)}/improvement/unitTypeImprovements/${improvementUnitTypeId}`,
            null,
            {
                errorHandler: (err, chain) => this._wicmHandleError(err, chain, { id: improvementUnitTypeId })
            }
        );
    }

    /**
     * Required because an error inside errorHandler produces an infinite loop (for unknown reasons :O)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns
     */
    private _getDisplayService(): DisplayService {
        if (!this._displayService) {
            throw new ProgrammingError(`The DisplayService was not injected into ${this.constructor.name}`);
        } else {
            return this._displayService;
        }
    }

    private _wicmHandleError(err: BackendError, chain: Observable<any>, item?: any): Observable<any> {
        if (err instanceof HttpErrorResponse) {
            const backendErr: BackendError = err.error;
            if (backendErr.message === 'I18N_ERR_NULL_IMPROVEMENT') {
                return of({});
            } else {
                if (!this._translateService || !item.type) {
                    if (item.type) {
                        this._wicmGetLog().warn('Can NOT translate because, TranslateService is not available');
                    }
                    this._getDisplayService().showBackendError(backendErr, item);
                } else {
                    const itemCopied = { ...item };
                    this._translateService.get(`IMPROVEMENTS.TYPES.${item.type}`).subscribe(translation => {
                        itemCopied.type = translation;
                        this._getDisplayService().showBackendError(backendErr, itemCopied);
                    });
                }
                return empty();
            }
        } else {
            return chain;
        }
    }

    private _wicmGetLog(): LoggerHelper {
        return new LoggerHelper(`${WithImprovementsCrudMixin.name} (${this._crudConfig.entityPath})`);
    }
}
