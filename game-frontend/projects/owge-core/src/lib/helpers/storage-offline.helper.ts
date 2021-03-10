import Dexie from 'dexie';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
export class StorageOfflineHelper<T> {
    private static readonly _PREFIX = 'owge_';

    private _browserStore: Storage | Dexie;
    private _dexieTable: Dexie.Table<T>;

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
     * @param [fields]
     */
    public constructor(private _storeName: string, storeType: 'local' | 'indexeddb' | 'session' = 'indexeddb', fields = '') {
        if (storeType === 'session') {
            this._browserStore = sessionStorage;
        } else if (storeType === 'local') {
            this._browserStore = localStorage;
        } else if (storeType === 'indexeddb') {
            this._browserStore = new Dexie(_storeName);
            this._browserStore.version(1).stores({
                data: fields
            });
            this._dexieTable = this._browserStore.table('data');
        }
    }

    /**
     * Saves to the localStorage
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param content
     */
    public async save(content: T): Promise<void> {
        if (this._browserStore instanceof Dexie) {
            await this._browserStore.transaction('rw', this._dexieTable, async () => {
                this._dexieTable.clear();
                this._dexieTable.add(content, 'store_data');
            });
        } else {
            this._browserStore.setItem(StorageOfflineHelper._PREFIX + this._storeName, JSON.stringify(content));
        }
    }

    /**
     * Returns the saved value or null if none
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async find(): Promise<T> {
        if (this._browserStore instanceof Storage) {
            const data: string = this._browserStore.getItem(StorageOfflineHelper._PREFIX + this._storeName);
            if (data) {
                return JSON.parse(data);
            } else {
                return null;
            }
        } else {
            return await this._dexieTable.get('store_data');
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
    public async doIfNotNull(action: (content: T) => void): Promise<void> {
        const value: T = await this.find();
        if (value) {
            await action(value);
        }
    }

    /**
     * Deletes the content
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public async delete(): Promise<void> {
        if (this._browserStore instanceof Dexie) {
            await this._dexieTable.clear();
        } else {
            this._browserStore.removeItem(StorageOfflineHelper._PREFIX + this._storeName);
        }
    }

    /**
     * Finds if a value exists, even if it's null
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns
     */
    public async isPresent(): Promise<boolean> {
        if (this._browserStore instanceof Dexie) {
            return (await this._dexieTable.count()) > 0;
        } else {
            return (StorageOfflineHelper._PREFIX + this._storeName) in this._browserStore;
        }
    }
}
