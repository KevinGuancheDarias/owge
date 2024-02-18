import { TranslatableTranslation } from './translatable-translation.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface Translatable {
    id: number;
    name: string;
    defaultLangCode: string;
    translation: TranslatableTranslation;
}
