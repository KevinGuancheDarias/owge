
/**
 * Applies a filter to a collection taking into acount the value to filter
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 * @template T Data type
 */
export interface WidgetFilter<T> {

    /**
     * Translatable name (shoule be a translation string)
     *
     * @since 0.9.0
     */
    name: string;

    /**
     * If the filter is enabled
     *
     * @since 0.9.0
     */
    isEnabled?: boolean;

    /**
     * When the filter doesn't use data, we can define the input we want to use <br>
     * <ul>
     * <li>text = for text input</li>
     * <li>number = for numeric input</li>
     * </ul>
     *
     * @since 0.10.0
     */
    inputType?: 'number' | 'text';

    /**
     * The data (required, if inputType is null)
     *
     * @since 0.9.0
     */
    data?: T[];

    /**
     * The selected field
     *
     * @since 0.9.0
     */
    selected?: T;

    /**
     * The action to run when filtering <br>
     * It's recommend to bind(this) before passing
     *
     * @param input The object to filter
     * @param selectedFilter The data from the filter
     * @since 0.9.0
     */
    filterAction: (input: any, selectedFilter: T) => Promise<boolean>;

    /**
     * Action to do to compare two selected values, defaults to id compare (if exists) <br>
     * It's recommend to bind(this) before passing
     *
     * @since 0.9.0
     */
    compareAction?: (a: T, b: T) => boolean;
}
