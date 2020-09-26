import { Injectable } from '@angular/core';
import { AbstractCrudService, Translatable, CrudServiceAuthControl, UniverseGameService, TranslatableTranslation } from '@owge/universe';
import { validContext } from '@owge/core';
import { Observable, of } from 'rxjs';

@Injectable()
export class AdminTranslatableService extends AbstractCrudService<Translatable> {

    public constructor(protected _universeGameService: UniverseGameService) {
        super(_universeGameService);
    }

    /**
     * Finds the allowed lang codes
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public findAllowedLangCodes(): Observable<string[]> {
        return of(['en', 'es']);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param translatableId
     * @returns
     */
    public findTranslations(translatableId: number): Observable<TranslatableTranslation[]> {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'get',
            `translatable/${translatableId}/translations`
        );
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param translatableId
     * @param translation
     * @returns
     */
    public addTranslation(translatableId: number, translation: TranslatableTranslation): Observable<TranslatableTranslation> {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'post',
            `translatable/${translatableId}/translations`,
            translation
        );
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param translationId
     * @returns
     */
    public deleteTranslation(translationId: number): Observable<void> {
        return this._universeGameService.requestWithAutorizationToContext(
            this._getContextPathPrefix(),
            'delete',
            `translatable/0/translations/${translationId}`
        );
    }

    protected _getEntity(): string {
        return 'translatable';
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
