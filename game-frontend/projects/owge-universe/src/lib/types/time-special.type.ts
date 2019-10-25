import { CommonEntity, EntityWithImage } from '@owge/core';

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
}
