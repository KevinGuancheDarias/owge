import { Injectable } from '@angular/core';
import { validContext } from '@owge/core';
import { AbstractCrudService, SystemMessage, UniverseGameService } from '@owge/universe';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 * @export
 */
@Injectable()
export class AdminSystemMessageService extends AbstractCrudService<SystemMessage> {

    public constructor(ugs: UniverseGameService) {
        super(ugs);
    }

    protected _getEntity(): string {
        return 'system-message';
    }

    protected _getContextPathPrefix(): validContext {
        return 'admin';
    }
}
