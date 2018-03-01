/**
 * Represents a User, as contained in the JWT token
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @interface User
 */
export interface User {
    id: number;
    username: string;
    email: string;
    password: string;
    activated: boolean;
    creationDate: number;
    lastLogin: number;
    firstName: string;
    lastName: string;
    notifications: boolean;
}
