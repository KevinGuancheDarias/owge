import { User } from '@owge/core';

export interface Audit {
    id: number;
    action: string;
    actionDetail: string;
    ip: string;
    ipv6: string;
    user: User;
}
