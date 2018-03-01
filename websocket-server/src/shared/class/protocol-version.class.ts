import { ProtocolVersionProperties } from '../types/protocol-version-properties.type';
import { ProgrammingError } from '../exception/programming.error';

export class ProtocolVersion {
    private static readonly VERSION_REGEX: RegExp = /^(\d*)\.(\d*)\.(\d*)$/g;

    private parsedVersion: ProtocolVersionProperties = <any>{};

    public static isValidVersion(target: string): boolean {
        const regularExpResult: string[] = this._execRegularExpression(target);
        return regularExpResult && regularExpResult.length === 4;
    }

    public static getInstance(version: string): ProtocolVersion {
        if (!ProtocolVersion.isValidVersion(version)) {
            throw new ProgrammingError('Can NOT create an instance of ProtocolVersio, please check that version has valid format');
        }
        const retVal: ProtocolVersion = new ProtocolVersion();
        [
            retVal.parsedVersion.major,
            retVal.parsedVersion.minor,
            retVal.parsedVersion.patch
        ] = ProtocolVersion.versionStringToArray(version);
        return retVal;
    }

    /**
     * Converts the input properties to a string
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @static
     * @param {ProtocolVersionProperties} protocolVersion
     * @returns {string} String representation, for example <b>0.1.0</b>
     * @memberof ProtocolVersion
     */
    public static convertPropertiesToString(protocolVersion: ProtocolVersionProperties): string {
        return protocolVersion.major + '.' + protocolVersion.minor + '.' + protocolVersion.patch;
    }

    private static versionStringToArray(version: string): number[] {
        const wantedVersion: string[] | number[] = this._execRegularExpression(version).splice(1);
        return wantedVersion.map(current => +current);
    }

    private static _execRegularExpression(version: string): string[] {
        ProtocolVersion.VERSION_REGEX.lastIndex = 0;
        return ProtocolVersion.VERSION_REGEX.exec(version);
    }

    private constructor() {
        // Can make instances only internally via the public getInstance method
    }

    public getParsedVersion(): ProtocolVersionProperties {
        return this.parsedVersion;
    }

    public isMajorEqualThan(compared: ProtocolVersionProperties): boolean {
        return this.parsedVersion.major === compared.major;
    }

    public isMajorDifferentThan(compared: ProtocolVersionProperties): boolean {
        return this.parsedVersion.major !== compared.major;
    }

    public isMinorDifferentThan(compared: ProtocolVersionProperties): boolean {
        return this.parsedVersion.minor !== compared.minor;
    }

    public isPatchDifferentThan(compared: ProtocolVersionProperties): boolean {
        return this.parsedVersion.patch !== compared.patch;
    }

}
