import { AbstractCrudService, CrudServiceAuthControl, TutorialSectionEntry, UniverseGameService } from '@owge/universe';
import { validContext } from '@owge/core';
import { Injectable } from '@angular/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminTutorialEntryService extends AbstractCrudService<TutorialSectionEntry> {

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    protected _getEntity(): string {
        return 'tutorial_section/entries';
    }

    protected _getContextPathPrefix(): validContext {
        return 'admin';
    }
    protected _getAuthConfiguration(): CrudServiceAuthControl {
        return {
            findAll: true,
            findById: true
        };
    }
}
