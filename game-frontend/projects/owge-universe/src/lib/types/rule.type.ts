import { Observable } from 'rxjs';

export interface Rule {
    id: number;
    type: string;
    originType: string;
    originId: number;
    destinationType: string;
    destinationId: number;
    destinationName$?: Observable<string>;
    extraArgs: any[];
}
