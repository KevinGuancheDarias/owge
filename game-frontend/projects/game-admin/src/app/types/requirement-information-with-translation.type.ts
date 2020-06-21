import { RequirementInformation } from '@owge/universe';

/**
 * Has frontend computed translations for the requirement
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface RequirementInformationWithTranslation extends RequirementInformation {
    translatedDescription?: string;
    resolvedName?: string;
    targetSecondValueTranslation?: string;
    targetThirdValueTranslation?: string;
}
