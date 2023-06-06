import { Component, OnInit, ViewChild, Output, EventEmitter, Input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { DisplayService } from '@owge/widgets';
import { ModalComponent } from '@owge/core';

import { ImageStoreService } from '../../services/image-store.service';
import { ImageStore } from '../../types/image-store.type';

/**
 * Allows selecting an image, or uploading a new one
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Component({
  selector: 'owge-universe-image-selector',
  templateUrl: './image-selector.component.html',
  styleUrls: ['./image-selector.component.less']
})
export class ImageSelectorComponent implements OnInit {
  @Input() public multiple = false;
  @Output() public selected: EventEmitter<ImageStore | ImageStore[]> = new EventEmitter();
  public images: ImageStore[];
  public selectedImage: ImageStore;
  public originalImage: ImageStore;

  @ViewChild('crudModal', { static: true }) protected _crudModal: ModalComponent;

  public constructor(
    private _translateService: TranslateService,
    private _displayService: DisplayService,
    private _imageStoreService: ImageStoreService
  ) {

  }

  ngOnInit() {
    this._imageStoreService.findAll().subscribe(images => this.images = images);
  }

  /**
   * Edits an image, usually to edit the Display Name or the Description
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param  el
   */
  public edit(el: ImageStore): void {
    this.selectedImage = el;
    this.originalImage = { ...el };
    this._crudModal.show();
  }

  /**
   * Deletes a selected element
   *
   * @todo This is a copy & paste of CommonCrudComponent.delete, in the future migrate that into a shared logic
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param el
   */
  public delete(el: ImageStore): void {
    this._translateService.get('CRUD.CONFIRM_DELETE', { elName: el.displayName }).subscribe(async val => {
      if (await this._displayService.confirm(val)) {
        this._imageStoreService.delete(el.id).subscribe(images => {
          this.images = images;
        });
      }
    });
  }

  public selectExisting(): void {
    alert('In future versions, maybe 0.10 or 0.11');
  }

  public uploadAndSelect(): void {
    const el: HTMLInputElement = document.createElement('input');
    el.type = 'file';
    el.multiple = this.multiple;
    el.onchange = async () => {
      if (el.files.length) {
        this.selected.emit(await this._imageStoreService.upload(this.multiple ? el.files : el.files[0]));
      }
    };
    el.click();
  }
}
