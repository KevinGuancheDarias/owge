import { Injectable } from '@angular/core';
import { take } from 'rxjs/operators';
import { Mixin } from 'ts-mixer';

import { LoadingService, validContext, OwgeCoreConfig } from '@owge/core';

import { WithReadCrudMixin } from '../mixins/services/with-read-crud.mixin';
import { UniverseGameService } from './universe-game.service';
import { ImageStore, CrudServiceAuthControl } from '@owge/types/universe';
import { WithDeleteCrudMixin } from '../mixins/services/with-delete-crud.mixin';

export interface ImageStoreService extends WithReadCrudMixin<ImageStore, number>, WithDeleteCrudMixin<ImageStore, number> { }

/**
 * Manages fetching and or save of a new image
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class ImageStoreService extends WithReadCrudMixin<ImageStore, number> {

    public constructor(
        protected _universeGameService: UniverseGameService,
        protected _loadingService: LoadingService,
        protected _config: OwgeCoreConfig
    ) {
        super();
    }

    public async upload(files: File): Promise<ImageStore>;
    public async upload(files: FileList | File): Promise<ImageStore[]>;

    /**
     * Saves one or multiple images
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param files Single file, or a list of files ( from elements input[type=files])
     * @returns
     */
    public async upload(files: FileList | File): Promise<any> {
        return await this._loadingService.runWithLoading(async () => {
            if (files instanceof FileList) {
                await Promise.all(Array.from(files).map(async file => await this._handleFileUpload(file)));
            } else {
                return await this._handleFileUpload(files);
            }
        });
    }

    protected _getEntity(): string {
        return 'image_store';
    }

    protected _getContextPathPrefix(): validContext {
        return this._config.contextPath;
    }

    protected _getAuthConfiguration(): CrudServiceAuthControl {
        return {
            findAll: true,
            findById: true
        };
    }

    private async _handleFileUpload(file: File): Promise<ImageStore> {
        const fileDataUrl: string = await this._readFile(file);
        return this._universeGameService.requestWithAutorizationToContext('admin', 'post', this._getEntity(), {
            displayName: file.name,
            base64: fileDataUrl
        }).pipe(take(1)).toPromise();
    }

    private _readFile(file: File): Promise<string> {
        return new Promise(resolve => {
            const reader: FileReader = new FileReader();
            reader.onloadend = () => resolve(<string>reader.result);
            reader.readAsDataURL(file);
        });
    }
}
(<any>ImageStoreService) = Mixin(WithDeleteCrudMixin, <any>ImageStoreService);
