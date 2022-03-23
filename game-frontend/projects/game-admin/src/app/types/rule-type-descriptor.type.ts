import { IdName } from '@owge/core';
import { Observable } from 'rxjs';

export interface RuleTypeDescriptor {
    name: string;
    extraArgs: Array<
        {
            name: string;
            formType: 'number' | 'text' | 'select';
            min?: number;
            data$?: Observable<IdName[]>;
            comparatorFn?: (a: IdName | number, b: IdName, number) => boolean;
        }>;
    allowedOrigins: string[];
}
