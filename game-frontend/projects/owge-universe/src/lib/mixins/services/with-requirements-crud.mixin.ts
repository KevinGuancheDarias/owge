import { ProgrammingError, LoggerHelper } from '@owge/core';

import { UniverseGameService } from '../../services/universe-game.service';
import { Observable } from 'rxjs';
import { CrudConfig } from '../../types/crud-config.type';
import { RequirementInformation } from '../../types/requirement-information.type';

/**
 * Add requirements handling to an existing crud service
 * <b>(backend context must always be 'admin')</b>
 * <b>_crudService must be defined in the parent class</b>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class WithRequirementsCrudMixin<K> {
    protected _universeGameService: UniverseGameService;
    protected _crudConfig: CrudConfig;

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

    private _findRequirementUrl(id: K): string {
        return `${this._crudConfig.findOneEntityPath(id)}/requirements`;
    }
}
