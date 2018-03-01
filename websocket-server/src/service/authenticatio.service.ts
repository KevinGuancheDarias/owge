import { User } from '../shared/types/user.type';
import { jws } from 'jsrsasign/lib/jsrsasign';

const JWS = jws.JWS;

export class AuthenticationService {
    private _validationSecret: string;

    /**
     * Creates an instance of AuthenticationService.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} [secret] Secret that was used
     * @memberof AuthenticationService
     */
    public constructor(secret?: string) {
        this._validationSecret = secret;
    }

    public setValidaitonSecret(secret: string): void {
        this._validationSecret = secret;
    }

    /**
     * Checks if the input token is valid
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} token JWT encoded token string
     * @returns {boolean}
     * @memberof AuthenticationService
     */
    public isValid(token: string): boolean {
        return this._isTokenWellFormed(token)
            && JWS.verify(token, { b64: this._validationSecret });
    }

    public findTokenUser(token: string): User {
        return JWS.parse(token).payloadObj.data;
    }

    private _isTokenWellFormed(token: string): boolean {
        try {
            return JWS.parse(token).payloadObj !== null;
        } catch (e) {
            console.warn('Malformed token:' + token, 'Error:', e);
            return false;
        }
    }
}
