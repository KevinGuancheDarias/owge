import { ProgrammingError, LoggerHelper } from '@owge/core';

import { UniverseGameService } from '../../services/universe-game.service';
import { Observable, Subject, Subscription, pipe } from 'rxjs';
import { CrudConfig } from '../../types/crud-config.type';
import { RequirementInformation } from '../../types/requirement-information.type';
import { HttpParams } from '@angular/common/http';
import { StoreAwareService } from '../../interfaces/store-aware-service.interface';
import { repeatWhen, take, finalize } from 'rxjs/operators';

/**
 * Add requirements handling to an existing crud service
 * <b>(backend context must always be 'admin')</b>
 * <b>_crudService must be defined in the parent class</b>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class WithRequirementsCrudMixin<T = any, K = any> implements StoreAwareService {
    protected _universeGameService: UniverseGameService;
    protected _crudConfig: CrudConfig;

    private _wrcmLog: LoggerHelper = new LoggerHelper(WithRequirementsCrudMixin.name);

    /**
     * Finds the requirements for the given entity key id
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param id
     * @returns
     */
    public findRequirements(id: K): Observable<RequirementInformation[]> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'get',
            `${this._crudConfig.findOneEntityPath(id)}/requirements`
        );
    }

    /**
     * Finds the data filted by requirements
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param requirementInformation
     * @returns
     */
    public findFilteredByRequirements(requirementInformation: RequirementInformation[]): Observable<T[]> {
        let params: HttpParams = new HttpParams();
        const names: string[] = requirementInformation.map(current => current.requirement.code);
        const secondValues: number[] = requirementInformation.map(current => current.secondValue);
        const thirdValues: number[] = requirementInformation.map(current => current.thirdValue);
        params = params.append('filterByRequirementName', names.join(','));
        params = params.append('filterByRequirementSecondValue', secondValues.join(','));
        params = params.append('filterByRequirementThirdValue', thirdValues.join(','));
        const createData = () => {
            const retVal: Observable<T[]> = this._universeGameService.requestWithAutorizationToContext(
                this._crudConfig.contextPath,
                'get',
                this._crudConfig.entityPath,
                null,
                { params }
            );
            retVal.pipe(take(1));
            return retVal;
        };
        if (this.getChangeObservable()) {
            const retVal: Subject<T[]> = new Subject();
            const subscription: Subscription = this.getChangeObservable().subscribe(async () => {
                retVal.next(await createData().toPromise());
            });
            return retVal.pipe(finalize(() => subscription.unsubscribe()));
        } else {
            this._wrcmLog.warn(`Service for entity ${this._crudConfig.entityPath} doesn't support filter auto-update`);
            return createData();
        }
        return createData();
    }

    /**
     * Saves the requirement to the backend
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param id
     * @param requirementInformation
     * @returns The saved requirement (with id included)
     */
    public saveRequirement(id: K, requirementInformation: RequirementInformation): Observable<RequirementInformation> {
        if (requirementInformation.id) {
            throw new ProgrammingError(`Can't save a requirement with has an id: ${requirementInformation.id}`);
        }
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'post',
            this._findRequirementUrl(id),
            requirementInformation
        );
    }

    /**
     * Deletes the specified requirement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param id Entity id
     * @param requirementId The id of the requirementInformation
     * @returns
     */
    public deleteRequirement(id: K, requirementId: number): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'delete',
            `${this._findRequirementUrl(id)}/${requirementId}`
        );
    }

    /**
     *Returns the store, the implementer should override this method, and return the Observable thay may contain the collection of entities
     * If not overriden, while will work, the requirements won't update
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns
     */
    public getChangeObservable(): Observable<any> {
        return null;
    }

    private _findRequirementUrl(id: K): string {
        return `${this._crudConfig.findOneEntityPath(id)}/requirements`;
    }
}
