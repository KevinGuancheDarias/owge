import { WidgetFilter } from '../types/widget-filter.type';

/**
 * Common filters
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 * @export
 */
export class WidgetFilterUtil {

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
            filterAction: async (el: { name: string }, inputText) => {
                return el.name && el.name.toLocaleLowerCase().includes(inputText.toLocaleLowerCase());
            }
        };
    }

    private constructor() {
        // Util class blabla...
    }
}
