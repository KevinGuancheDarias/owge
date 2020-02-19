import { Component, OnInit, Input, ContentChild, TemplateRef, ViewChild, Output, EventEmitter } from '@angular/core';

import { CommonEntity, LoadingService } from '@owge/core';
import { AbstractCrudService } from '@owge/universe';
import { ModalComponent } from '@owge/core';
import { DisplayService } from '@owge/widgets';
import { TranslateService } from '@ngx-translate/core';
import { isEqual } from 'lodash-es';

/**
 * Used to handle the default Crud <br>
 * Possible ng-content selectors are
 * <ul>
 * <li> .middle = Usually used to display an image by CommonCrudWithImageComponent
 * </ul>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 * @template T
 * @template K
 */
@Component({
  selector: 'app-common-crud',
  templateUrl: './common-crud.component.html',
  styleUrls: ['./common-crud.component.less']
})
export class CommonCrudComponent<K, T extends CommonEntity<K>> implements OnInit {

  @ContentChild('modalBody', { static: true }) public modalBody: TemplateRef<any>;
  @ContentChild('middleOfCard', { static: true }) public middleOfCard: TemplateRef<any>;
  @Input() public hasDescription = true;
  @Output() public elementsLoaded: EventEmitter<void> = new EventEmitter;
  @Output() public elementSelected: EventEmitter<T> = new EventEmitter;
  public elements: T[];
  public newElement: T;
  public originalElement: T;
  public isChanged: boolean;

  @Input() protected _crudService: AbstractCrudService<T, K>;
  @ViewChild('crudModal', { static: true }) protected _crudModal: ModalComponent;
  protected _randomId: string;

  constructor(
    protected _displayService: DisplayService,
    protected _translateService: TranslateService,
    protected _loadingService: LoadingService
  ) { }

  ngOnInit() {
    this._randomId = (new Date()).getTime().toString();
    if (!this.elements) {
      this._crudService.findAll().subscribe(elements => {
        this.elements = elements;
        this.elementsLoaded.emit();
      });
    }
  }

  /**
   * Edits the element
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param  el
   */
  public edit(el: T): void {
    this.originalElement = el;
    this.newElement = { ...el };
    this.elementSelected.emit(this.newElement);
    this._crudModal.show();
  }

  /**
   * Creates a new element
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public new(): void {
    this.newElement = <any>{};
    this.originalElement = { ...this.newElement };
    this.elementSelected.emit(this.newElement);
    this._crudModal.show();
  }

  /**
   * Deletes a selected element
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param el
   */
  public delete(el: T): void {
    this._translateService.get('CRUD.CONFIRM_DELETE', { elName: el.name }).subscribe(async val => {
      if (await this._displayService.confirm(val)) {
        this._crudService.delete(el.id).subscribe(elements => {
          this.elements = elements;
          this._crudModal.hide();
        });
      }
    });
  }

  /**
   * Generates a random id for HTML elements (to avoid surprises, if multiple <i>CommonCrudComponent</i> are present in the DOM)
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   * @param  target
   * @returns
   */
  public uniqueId(target: string): string {
    return `${target}_${this._randomId}`;
  }

  /**
   * Saves the entity to the backend
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public save(): void {
    this._loadingService.runWithLoading(async () => {
      if (this.newElement.id) {
        this.newElement = await this._crudService.saveExistingOrPut(this.newElement).toPromise();
      } else {
        this.newElement = await this._crudService.saveNew(this.newElement).toPromise();
      }
      this.originalElement = { ...this.newElement };
      this._crudModal.hide();
    });
  }

  /**
   * Cancels the current modal contents, and hides it
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public cancel(): void {
    this._translateService.get('CRUD.CONFIRM_CANCEL').subscribe(async val => {
      if (!this.isChanged || await this._displayService.confirm(val)) {
        this._crudModal.hide();
      }
    });
  }

  /**
   *
   * @todo In the future don't invoke this method from the template
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.0
   */
  public detectIsChanged(): boolean {
    this.isChanged = !isEqual(this.newElement, this.originalElement);
    return this.isChanged;
  }
}
