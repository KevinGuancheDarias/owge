import { TokenPojo } from '../pojos/token.pojo';
import { User } from '@owge/types/core';
export class JwtTokenUtil {


    /**
     * Parses a JWT token an return its data
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @template U
     * @param  rawJwtToken
     * @returns
     * @memberof JwtTokenUtil
     */
    public static parseToken<U extends User = User>(rawJwtToken: string): TokenPojo {
        if (rawJwtToken) {
            const retVal: TokenPojo = JSON.parse(atob(rawJwtToken.split('.')[1]));
            retVal.exp *= 1000;
            return retVal;
        } else {
            return null;
        }
    }


    /**
     * Returns parsed token if not expired
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  rawToken (The raw token, that can be obtained from any source)
     * @param [expiredAction=() => {}] Action to execute if the token is expired
     * @returns The parsed token
     * @memberof JwtTokenUtil
     */
    public static findTokenIfNotExpired(rawToken: string, expiredAction: () => void = () => {}): TokenPojo {
        let retVal: TokenPojo;
        if (rawToken) {
          const parsedToken: TokenPojo = JwtTokenUtil.parseToken(rawToken);
          if (parsedToken) {
            if (!TokenPojo.isExpired(parsedToken)) {
              retVal = parsedToken;
            } else {
              expiredAction();
            }
          }
        }
        return retVal;
    }

    private constructor() {
        // Util class doesn't have a constructor
    }
}
