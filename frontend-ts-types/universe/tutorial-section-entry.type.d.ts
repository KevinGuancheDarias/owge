import { TutorialSectionAvailableHtmlSymbol } from './tutorial-section-available-html-symbol.type';
import { Translatable } from './translatable.type';
import { TutorialEvent } from '../core';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface TutorialSectionEntry {
    id: number;
    order: number;
    htmlSymbol: TutorialSectionAvailableHtmlSymbol;
    event: TutorialEvent;
    text: Translatable;
}
