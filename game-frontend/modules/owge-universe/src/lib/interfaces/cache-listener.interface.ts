/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.15
 * @export
 */
export interface CacheListener {

    /**
     * Runs listener after
     *
     * @since 0.9.15
     */
    afterCacheClear(): Promise<void>;
}
