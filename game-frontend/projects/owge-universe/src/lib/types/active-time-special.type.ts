
/**
 * Represents an activated type special
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface ActiveTimeSpecialType {
    id: number;
    timeSpecial: number;
    state: 'ACTIVE' | 'RECHARGE';
    activationDate: number;
    expiringDate: number;
    readyDate: number;
    pendingTime: number;
}
