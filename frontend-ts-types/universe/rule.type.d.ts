
export interface Rule {
    id: number;
    type: string;
    originType: string;
    originId: number;
    destinationType: string;
    destinationId: number;
    extraArgs: any[];
}
