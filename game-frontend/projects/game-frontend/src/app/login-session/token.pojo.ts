import { UserPojo } from '../shared-pojo/user.pojo';

export class TokenPojo {
    public sub: number;
    public data: UserPojo;
    public exp: number;
    public iat: number;

    public static isExpired(token: TokenPojo): boolean {
        return new Date().getTime() > token.exp;
    }
}
