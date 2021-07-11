
/**
 * Has the "async" version of array filtration methods
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class AsyncCollectionUtil {

    private constructor() {
        // Util class doesn't have a constructor
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     * @static
     * @template T
     * @param collection
     * @param action
     */
    public static async forEach<T>(collection: T[], action: (entry: T) => Promise<void>) {
        for (const entry of collection) {
            await action(entry);
        }
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @static
     * @template T
     * @param collection
     * @param action
     * @returns
     */
    public static async filter<T>(collection: T[], action: (entry: T) => Promise<boolean>): Promise<T[]> {
        const retVal: T[] = [];
        for (const entry of collection) {
            if (await action(entry)) {
                retVal.push(entry);
            }
        }
        return retVal;
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @template T
     * @param collection
     * @param action
     * @returns
     */
    public static async every<T>(collection: T[], action: (entry: T) => Promise<boolean>): Promise<boolean> {
        for (const entry of collection) {
            if (!(await action(entry))) {
                return false;
            }
        }
        return true;
    }

}
