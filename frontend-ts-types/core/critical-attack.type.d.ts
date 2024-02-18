import { CriticalAttackEntry } from './critical-attack-entry.type';

export interface CriticalAttack {
    id: number;
    name: string;
    entries: CriticalAttackEntry[];
}
