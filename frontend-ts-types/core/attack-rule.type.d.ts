import { AttackRuleEntry } from './attack-rule-entry.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface AttackRule {
    id: number;
    name: string;
    entries: AttackRuleEntry[];
}
