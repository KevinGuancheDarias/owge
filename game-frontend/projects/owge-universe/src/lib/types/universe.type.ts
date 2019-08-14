/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface Universe {
    id: number;
    name: string;
    description?: string;
    creationDate?: Date;
    is: boolean;
    oficial?: boolean;
    restBaseUrl?: string;
    frontendUrl?: string;
}
