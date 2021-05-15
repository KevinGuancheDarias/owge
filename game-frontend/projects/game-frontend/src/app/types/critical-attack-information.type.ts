export interface CriticalAttackInformation {
    target: 'UNIT' | 'UNIT_TYPE';
    targetId: number;
    targetName: string;
    value: number;
}
