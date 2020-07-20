
/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface AttackRuleEntry {
    target: 'UNIT' | 'UNIT_TYPE';
    referenceId: number;
    canAttack: boolean;
}
