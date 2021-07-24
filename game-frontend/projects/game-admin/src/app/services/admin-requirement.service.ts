import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { AdminFactionService } from './admin-faction.service';
import { AdminUpgradeService } from './admin-upgrade.service';
import { Observable } from 'rxjs';
import { RequirementInformationWithTranslation } from '../types/requirement-information-with-translation.type';
import { take } from 'rxjs/operators';
import { ProgrammingError, RequirementInformation } from '@owge/core';
import { AdminSpecialLocationService } from './admin-special-location.service';
import { AdminUnitService } from './admin-unit.service';
import { AdminTimeSpecialService } from './admin-time-special.service';


/**
 * Has methods to manipulate requirements (no backend manipulation)
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminRequirementService {
    public constructor(
        private _translateService: TranslateService,
        private _adminFactionService: AdminFactionService,
        private _adminUpgradeService: AdminUpgradeService,
        private _adminSpecialLocationService: AdminSpecialLocationService,
        private _adminUnitService: AdminUnitService,
        private adminTimeSpecialService: AdminTimeSpecialService
    ) { }

    /**
     * Returns the translation for a requirement <br>
     * Notice: It's safe to use this in the view, as the returned observable is always the same
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param code
     * @param [context] When passed it's the context of the requirement (used to replace placeholders in the translations)
     * @returns
     */
    public findRequirementDescription(code: string, context?: RequirementInformation): Observable<string> {
        return this._translateService.get(`REQUIREMENTS.DESCRIPTIONS.${code}`, context);
    }

    /**
     * Handles the translation of a requirement information, and returns it with its translation properties
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param requirementInformation
     * @returns
     */
    public handleRequirementTranslation(requirementInformation: RequirementInformation): RequirementInformationWithTranslation {
        const retVal: RequirementInformationWithTranslation = {
            ...requirementInformation,
            translatedDescription: 'Loading...',
            resolvedName: 'Loading...'
        };
        this._findTranslatedDescription(requirementInformation).subscribe(result => retVal.translatedDescription = result);
        this._resolveTargetName(requirementInformation).then(result => retVal.resolvedName = result);
        return retVal;
    }

    private _findTranslatedDescription(requirementInformation: RequirementInformation): Observable<string> {
        return this.findRequirementDescription(requirementInformation.requirement.code, requirementInformation);
    }

    private async _resolveTargetName(requirementInformation: RequirementInformation): Promise<string> {
        switch (requirementInformation.requirement.code) {
            case 'BEEN_RACE':
                return (await this._adminFactionService.findOneById(requirementInformation.secondValue).pipe(take(1)).toPromise()).name;
            case 'UPGRADE_LEVEL':
                return (await this._adminUpgradeService.findOneById(requirementInformation.secondValue).pipe(take(1)).toPromise()).name;
            case 'HAVE_SPECIAL_LOCATION':
                return (await this._adminSpecialLocationService.findOneById(requirementInformation.secondValue)
                    .pipe(take(1)).toPromise()).name;
            case 'HAVE_UNIT':
                return (await this._adminUnitService.findOneById(requirementInformation.secondValue)
                    .pipe(take(1)).toPromise()).name;
            case 'HAVE_SPECIAL_ENABLED':
                return (await this.adminTimeSpecialService.findOneById(requirementInformation.secondValue)
                    .pipe(take(1)).toPromise()).name;
            default:
                throw new ProgrammingError(`Invalid requirement code ${requirementInformation.requirement.code}`);
        }
    }
}
