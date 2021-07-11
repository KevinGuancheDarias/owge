import { Component, ContentChild, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, TemplateRef, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { CommonEntity, LoadingService, ModalComponent } from '@owge/core';
import { AbstractCrudService } from '@owge/universe';
import { DisplayService, WidgetFilter } from '@owge/widgets';
import { isEqual } from 'lodash-es';
import { Observable, Subscription } from 'rxjs';


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
export class CommonCrudComponent<K, T extends CommonEntity<K>> implements OnInit, OnChanges, OnDestroy {

  @ContentChild('beforeList', { static: true }) public beforeList: TemplateRef<any>;
  @ContentChild('modalBody', { static: true }) public modalBody: TemplateRef<any>;
  @ContentChild('middleOfCard', { static: true }) public middleOfCard: TemplateRef<any>;
  @Input() public hasDescription = true;
  @Input() public idField: keyof T = 'id';
  @Input() public hideSections: { id?: boolean; name?: boolean; description?: boolean };
  @Input() public customElementsSource: Observable<T[]>;
  @Input() public customNewFiller: (el: T) => Promise<T>;
  @Input() public customSaveAction: (el: T) => Promise<T>;
  @Input() public allowSelection = false;
  @Input() public disableName = false;
  @Input() public displayFilter = false;
  @Input() public customFilters: WidgetFilter<any>[] = [];
  @Input() public noDefaultFilter = false;
  @Output() public elementsLoaded: EventEmitter<T[]> = new EventEmitter;
  @Output() public elementSelected: EventEmitter<T> = new EventEmitter;
  @Output() public saveResult: EventEmitter<T> = new EventEmitter;

  /**
   * When allowSelection is defined, can be used to control selection <br>
   * <b>NOTICE:</b> don't confuse with elementSelected which is used to control the creation/edition modal
   *
   * @since 0.9.0
   */
  @Output() public choosen: EventEmitter<T> = new EventEmitter;
  public elements: T[];
  public newElement: T;
  public originalElement: T;
  public isChanged: boolean;
  public filteredElements: T[];

  @Input() protected _crudService: AbstractCrudService<T, K>;
  @ViewChild('crudModal', { static: true }) protected _crudModal: ModalComponent;
  protected _randomId: string;

  private _defaultSubscription: Subscription;
  private _customSubscription: Subscription;
  constructor(
    protected _displayService: DisplayService,
    protected _translateService: TranslateService,
    protected _loadingService: LoadingService
  ) { }

  ngOnInit() {
    this._randomId = (new Date()).getTime().toString();
  }

  public ngOnChanges(): void {
    const onSubscribeAction = elements => {
      this.elements = elements;
      this.filteredElements = elements;
      this.elementsLoaded.emit(elements);
    };
    if (this.customElementsSource) {
      if (this._customSubscription) {
        this._customSubscription.unsubscribe();
      }
      this._customSubscription = this.customElementsSource.subscribe(onSubscribeAction);
      if (this._defaultSubscription) {
        this._defaultSubscription.unsubscribe();
        delete this._defaultSubscription;
      }
    } else {
      if (!this._defaultSubscription) {
        this._defaultSubscription = this._crudService.findAll().subscribe(onSubscribeAction);
      }
      if (this._customSubscription) {
        this._customSubscription.unsubscribe();
        delete this._customSubscription;
      }
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
  public async new(): Promise<void> {
    this.newElement = {} as any;
    if (this.customNewFiller) {
      this.newElement = await this.customNewFiller(this.newElement);
    }
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
        this._crudService.delete(el[this.idField as any]).subscribe(elements => {
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
      this.saveResult.emit(this.customSaveAction
        ? await this.customSaveAction(this.newElement)
        : await this._doSave()
      );
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


  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public ngOnDestroy(): void {
    if (this._customSubscription) {
      this._customSubscription.unsubscribe();
    } else if (this._defaultSubscription) {
      this._defaultSubscription.unsubscribe();
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param el
   */
  public clickSelect(el: T): void {
    this.choosen.emit({ ...el });
  }

  /**
   * Updates the original
   * <br>
   * Must be invoked by outside's component
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.10.0
   * @param newOriginal
   */
  public updateOriginal(newOriginal: T): void {
    this.originalElement = { ...newOriginal };
  }

  /**
   * Do the savings to the backend
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @protected
   * @returns
   * @since 0.9.0
   */
  protected async _doSave(): Promise<T> {
    if (this.newElement[this.idField]) {
      this.newElement = await this._crudService.saveExistingOrPut(this.newElement).toPromise();
    } else {
      this.newElement = await this._crudService.saveNew(this.newElement).toPromise();
    }
    this.originalElement = { ...this.newElement };
    this._crudModal.hide();
    return this.newElement;
  }
}
