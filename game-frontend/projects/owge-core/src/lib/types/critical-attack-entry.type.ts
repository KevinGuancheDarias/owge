export interface CriticalAttackEntry {
    id?: number;
    target: 'UNIT' | 'UNIT_TYPE';
    referenceId: number;
    value: number;
}
