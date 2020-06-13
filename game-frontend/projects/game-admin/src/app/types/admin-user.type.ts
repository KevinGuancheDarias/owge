
/**
 * Represents a backend already-added admin user
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface AdminUser {
    id: number;
    username: string;
    enabled: boolean;
    canAddAdmins: boolean;
}
