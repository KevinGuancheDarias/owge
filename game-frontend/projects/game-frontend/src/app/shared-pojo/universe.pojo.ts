
/**
 *
 *
 * @deprecated As of 0.8.0 It's better to use the Universe type from OwgeUniverseModule
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class Universe
 */
export class Universe {
    public id: number;
    public name: string;
    public description?: string;
    public creationDate?: Date;
    public isPublic?: boolean;
    public oficial?: boolean;
    public restBaseUrl?: string;
    public frontendUrl?: string;
}
