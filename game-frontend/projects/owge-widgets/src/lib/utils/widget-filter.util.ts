import { ProgrammingError } from '@owge/core';
import { WithRequirementsCrudMixin } from '@owge/universe';
import { RequirementInformation } from 'projects/owge-core/src/lib/types/requirement-information.type';
import { take } from 'rxjs/operators';
import { WidgetFilter } from '../types/widget-filter.type';

/**
 * Common filters
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 * @export
 */
export class WidgetFilterUtil {

    private constructor() {
        // Util class blabla...
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     * @returns
     */
    public static buildByNameFilter(): WidgetFilter<string> {
        return {
            name: 'FILTER.BY_NAME',
            inputType: 'text',
            filterAction: async (el: { name: string }, inputText) =>
                el.name && el.name.toLocaleLowerCase().includes(inputText.toLocaleLowerCase())
        };
    }

    public static async loadRequirementsIfRequired(
        input: { id: number; requirements: RequirementInformation[]},
        fetcher?: WithRequirementsCrudMixin
    ): Promise<void> {
        if(!input.requirements && fetcher) {
            input.requirements = await fetcher.findRequirements(input.id).pipe(take(1)).toPromise();
        }
    }

    public static runRequirementsFilter(
        input: {requirements: RequirementInformation[]},
        filterAction: (requirement: RequirementInformation) => boolean
    ): boolean {
        const requirements: RequirementInformation[] = input.requirements;
        if (!requirements) {
            throw new ProgrammingError('Can NOT filter when the input has not requirements');
        }
        return requirements.some(requirement => filterAction(requirement));
    }

}
