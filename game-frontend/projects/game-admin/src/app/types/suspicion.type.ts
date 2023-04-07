import { User } from '@owge/core';
import { Audit } from './audit.type';

export interface Suspicion {
    id: number;
    source: string;
    user: User;
    audit: Audit;
    createdAt: number;
}
