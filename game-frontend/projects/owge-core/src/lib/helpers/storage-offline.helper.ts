import * as LZString from 'lz-string';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
export class StorageOfflineHelper<T> {
    private static readonly _PREFIX = 'owge_';

    private _browserStore: Storage;

    /**
     * Gets the prefix used for indexding the content
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public static getStartingPrefix(): string {
        return StorageOfflineHelper._PREFIX;
    }
    /**

     * Creates an instance of StorageOfflineHelper.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param _storeName
     * @param [storeType] When not session must be used with caution, usually from UniverseCacheManagerService
     */
    public constructor(private _storeName: string, storeType: 'local' | 'session' = 'session') {
        if (storeType === 'session') {
            this._browserStore = sessionStorage;
        } else if (storeType === 'local') {
            this._browserStore = localStorage;
        }
    }

    /**
     * Saves to the localStorage
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param content
     */
    public save(content: T): void {
        this._browserStore.setItem(StorageOfflineHelper._PREFIX + this._storeName, LZString.compress(JSON.stringify(content)));
    }

    /**
     * Returns the saved value or null if none
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public find(): T {
        const data: string = this._browserStore.getItem(StorageOfflineHelper._PREFIX + this._storeName);
        if (data) {
            return JSON.parse(LZString.decompress(data));
        } else {
            return null;
        }
    }

    /**
     * Runs action if value is not null <br>
     * Used to avoid multiple silly "if-not-null"
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param action
     */
    public doIfNotNull(action: (content: T) => void): void {
        const value: T = this.find();
        if (value) {
            action(value);
        }
    }

    /**
     * Deletes the content
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public delete(): void {
        this._browserStore.removeItem(StorageOfflineHelper._PREFIX + this._storeName);
    }
}
