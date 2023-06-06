import { ProgrammingError, LoggerHelper, RequirementInformation, RequirementGroup } from '@owge/core';

import { UniverseGameService } from '../../services/universe-game.service';
import { Observable, Subject, Subscription, pipe } from 'rxjs';
import { CrudConfig } from '../../types/crud-config.type';
import { HttpParams } from '@angular/common/http';
import { StoreAwareService } from '../../interfaces/store-aware-service.interface';
import { take, finalize } from 'rxjs/operators';

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
     * Finds requirement group
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param {K} id
     * @returns {Observable<RequirementGroup>}
     */
    public findRequirementGroups(id: K): Observable<RequirementGroup> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'get',
            `${this._crudConfig.findOneEntityPath(id)}/requirement-group`
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
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param ownerId
     * @param name
     * @returns
     */
    public addGroup(ownerId: number, name: string): Observable<RequirementGroup> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'post',
            `${this._crudConfig.findOneEntityPath(ownerId)}/requirement-group`,
            { name }
        );
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param id
     * @param groupId
     * @param requirementInformation
     * @returns
     */
    public addRequirementToGroup(
        id: K,
        groupId:
            number, requirementInformation: RequirementInformation
    ): Observable<RequirementInformation> {
        if (requirementInformation.id) {
            throw new ProgrammingError(`Can't save a requirement with has an id: ${requirementInformation.id}`);
        }
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'post',
            this._findRequirementGroupUrl(id, groupId),
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
     * Delete a requirement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param id
     * @param groupId
     * @param requirementId
     * @returns
     */
    public deleteRequirementByGroupAndId(id: K, groupId: number, requirementId: number): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'delete',
            `${this._findRequirementGroupUrl(id, groupId)}/${requirementId}`
        );
    }

    /**
     * Deletes a group
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param id
     * @param groupId
     * @returns
     */
    public deleteRequirementGroup(id: K, groupId: number): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            'admin',
            'delete',
            `${this._crudConfig.findOneEntityPath(id)}/requirement-group/${groupId}`
        );
    }

    /**
     * Returns the store, the implementer should override this method, and return the Observable thay may contain the collection of entities
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

    private _findRequirementGroupUrl(id: K, groupId: number) {
        return `${this._crudConfig.findOneEntityPath(id)}/requirement-group/${groupId}/requirement`;
    }
}
