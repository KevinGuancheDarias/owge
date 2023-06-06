import { CommonEntity, EntityWithImage, Improvement } from '@owge/core';

import { ActiveTimeSpecialType } from './active-time-special.type';

/**
 * Represents a Time special
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface TimeSpecial extends CommonEntity<number>, EntityWithImage {
    duration: number;
    rechargeTime: number;
    improvement: Improvement;
    activeTimeSpecialDto?: ActiveTimeSpecialType;
}
