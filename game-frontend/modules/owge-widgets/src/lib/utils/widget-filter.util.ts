import { WidgetFilter } from '../types/widget-filter.type';

/**
 * Common filters
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 * @export
 */
export class WidgetFilterUtil {
    private static readonly MAX_DEPTH = 100;
    private static readonly SEPARATOR = '.';

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
        return WidgetFilterUtil.buildByFieldFilter('FILTER.BY_NAME', 'text', 'name');
    }

    public static buildByFieldFilter(filterName: string, inputType: 'text'| 'number', field: string ): WidgetFilter<string> {
        return {
            name: filterName,
            inputType,
            filterAction: async (el: any, inputText) => {
                const fieldValue = WidgetFilterUtil.traverseObjectForStringProp(field, el);
                return typeof fieldValue === 'string' && fieldValue.toLocaleLowerCase().includes(inputText.toLocaleLowerCase());
            }
        };
    }

    private static traverseObjectForStringProp(path: string, el: unknown, maxDepth = 0): unknown {
        if(maxDepth > WidgetFilterUtil.MAX_DEPTH) {
            console.warn('surrendered trying to solve path for ', path, el);
            return '';
        }
        const pathParts = path.split(WidgetFilterUtil.SEPARATOR);
        if(pathParts.length) {
            const currentPart = pathParts[0];
            if(typeof el[currentPart] !== 'object') {
                return el[currentPart];
            } else {
                pathParts.shift();
                return this.traverseObjectForStringProp(pathParts.join(WidgetFilterUtil.SEPARATOR), el[currentPart], maxDepth +1) ;
            }
        } else {
            return el[path];
        }
    }
}
