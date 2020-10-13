import {
    AbstractCrudService, TutorialSection, CrudServiceAuthControl, UniverseGameService,
    TutorialSectionAvailableHtmlSymbol,
    TutorialSectionEntry
} from '@owge/universe';
import { validContext } from '@owge/core';
import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class AdminTutorialService extends AbstractCrudService<TutorialSection> {
    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findHtmlSymbols(): Observable<TutorialSectionAvailableHtmlSymbol[]> {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'get',
            'tutorial_section/availableHtmlSymbols'
        );
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findEntries(): Observable<TutorialSectionEntry[]> {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'get',
            'tutorial_section/entries'
        );
    }

    protected _getEntity(): string {
        return 'tutorial_section';
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
