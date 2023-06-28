
/**
 * Represents an activated time special
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface ActiveTimeSpecial {
    id: number;
    timeSpecial: number;
    state: 'ACTIVE' | 'RECHARGE';
    activationDate: number;
    expiringDate: number;
    readyDate: number;

    /**
     * @deprecated As of 0.9.0 the prop name should be pendingMillis
     */
    pendingTime: number;

    pendingMillis: number;
    browserComputedTerminationDate: Date;
}
