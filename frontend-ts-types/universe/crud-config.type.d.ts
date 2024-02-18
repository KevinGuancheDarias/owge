import { CrudServiceAuthControl } from './crud-service-auth-control.type';

export type validContext = 'game' | 'admin' | 'open';

/**
 * Represents the current configuration of a Crud extending <i>AbstractCrudService</i>
 * Used to avoid making public a lot of methods
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface CrudConfig {
    entityPath: string;
    contextPath: validContext;
    authConfiguration: CrudServiceAuthControl;
    findOneEntityPath: (id: any) => string;
}

