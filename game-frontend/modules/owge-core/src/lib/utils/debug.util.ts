
/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class DebugUtil {


    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public static getStacktrace(): string {
        try {
            throw new Error();
        } catch (e) {
            return e.stack;
        }
    }
    private constructor() {
        // An util class doesn't have a constructor
    }
}
