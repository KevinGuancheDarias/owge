import { TutorialSectionAvailableHtmlSymbol } from './tutorial-section-available-html-symbol.type';
import { TutorialEvent } from 'projects/owge-core/src/lib/types/tutorial-event.type';
import { Translatable } from './translatable.type';


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
