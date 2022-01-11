export interface RuleTypeDescriptor {
    name: string;
    extraArgs: Array<{ name: string; formType: 'number' | 'text'; min?: number }>;
}
