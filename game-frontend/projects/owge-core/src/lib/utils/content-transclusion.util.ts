import { QueryList, TemplateRef } from '@angular/core';
import { OwgeContentDirective } from '../directives/owge-content.directive';

/**
 * Helper methods to handle interaction with OwgeContentDirective
 *
 * @see {OwgeContentDirective}
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
export class ContentTransclusionUtil {

    /**
     * Finds a template with a specified selector
     *
     * @see WidgetDisplayListItemComponent
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     * @template C
     * @param list
     * @param selector
     * @returns The template, or null if none exists
     */
    public static findInList<C = any>(list: QueryList<OwgeContentDirective<C>>, selector: string): TemplateRef<C> {
        const directive: OwgeContentDirective<C> = list.find(current => current.select === selector);
        return directive ? directive.templateRef : null;
    }

    private constructor() {
        // Util class doesn't have a constructor
    }
}
