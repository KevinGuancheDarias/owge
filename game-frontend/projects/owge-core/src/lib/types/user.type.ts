/**
 * Better way to represent user details
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
export interface User {
    id: number;
    username: string;
    email: string;
    password: string;
    activated: boolean;
    creationDate: Date;
    lastLogin: Date;
    firstName: string;
    lastName: string;
    notifications: boolean;

    consumedEnergy: number;
    maxEnergy: number;
    primaryResource: number;
    secondaryResource: number;
}
