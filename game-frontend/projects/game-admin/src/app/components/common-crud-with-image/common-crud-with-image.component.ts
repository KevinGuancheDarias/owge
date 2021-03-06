import { Component, OnInit, ContentChild, TemplateRef, OnChanges, ViewChild } from '@angular/core';

import { EntityWithImage, CommonEntity } from '@owge/core';
import { AbstractCrudService, ImageStore } from '@owge/universe';

import { CommonCrudComponent } from '../common-crud/common-crud.component';

@Component({
  selector: 'app-common-crud-with-image',
  templateUrl: './common-crud-with-image.component.html',
  styleUrls: ['./common-crud-with-image.component.less']
})
export class CommonCrudWithImageComponent<K, T extends CommonEntity<K> & EntityWithImage> extends CommonCrudComponent<K, T>
  implements OnInit, OnChanges {

  @ViewChild(CommonCrudComponent) public commonCrudComponent: CommonCrudComponent<K, T>;
  @ContentChild('beforeList', { static: true }) public innerBeforeList: TemplateRef<any>;
  @ContentChild('modalBody', { static: true }) public innerModalBody: TemplateRef<any>;
  public viewExposedService: AbstractCrudService<T, K>;

  public ngOnInit() {
    this.viewExposedService = this._crudService;
  }

  public ngOnChanges(): void {
    // Override CommonCrudComponent.ngOnChanges to disable it
  }

  /**
   * Defines the image for the input <i>newElement</i>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param  newElement
   * @param  image
   */
  public setImage(newElement: T, image: ImageStore): void {
    if(image) {
      newElement.image = image.id;
      newElement.imageUrl = image.url;
    } else {
      newElement.image = null;
      newElement.imageUrl = null;
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.10.0
   * @param original
   */
  public updateOriginal(original: T): void {
    this.commonCrudComponent.updateOriginal(original);
  }
}
