
/**
 * If properties are true will mean that <b>authentication</b> is required
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface CrudServiceAuthControl {
    findAll: boolean;
    findById: boolean;
}
