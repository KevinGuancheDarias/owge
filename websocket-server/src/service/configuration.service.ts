import { BadDataError } from '../shared/exception/bad-data.error';
import * as mysql from 'mysql';
import * as mysqlPromise from 'promise-mysql';

/**
 * Class used to access the JWT configuration options from the database
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class ConfigurationService
 */
export class ConfigurationService {
    private static readonly SETTING_NAME = 'JWT_SECRET';

    private _connectionPool: mysqlPromise.Pool;

    public constructor(connectionConfiguration: mysql.PoolConfig) {
        this._connectionPool = mysqlPromise.createPool(connectionConfiguration);
    }

    /**
     * Returns the JWT secret used to validate the tokens <br>
     * In the future, I will use public/private keys for this purpose
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns {Promise<string>}
     * @memberof ConfigurationService
     */
    public async findJwtSecret(): Promise<string> {
        const result = await (await this._getConnection()).query('SELECT value FROM configuration ' +
            'WHERE name = \'' + ConfigurationService.SETTING_NAME + '\'');
        if (result.length !== 1) {
            throw new BadDataError('Missing value for ' + ConfigurationService.SETTING_NAME);
        }
        return result[0].value;
    }

    private async _getConnection(): Promise<mysqlPromise.Connection> {
        return await this._connectionPool.getConnection();
    }
}
