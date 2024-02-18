import { User } from '@owge/types/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @template U
 */
export class TokenPojo<U extends User = User> {
    public sub: number;
    public data: U;
    public exp: number;
    public iat: number;

    public static isExpired(token: TokenPojo): boolean {
        return new Date().getTime() > token.exp;
    }
}
