import { Component, OnInit, Input, Output, EventEmitter, ViewChild } from '@angular/core';
import { Translatable, TranslatableTranslation } from '@owge/universe';
import { AdminTranslatableService } from '../../services/admin-translatable.service';
import { Observable } from 'rxjs';
import { WidgetChooseItemModalComponent } from '@owge/widgets';

interface TranslationsWithDirtyState extends TranslatableTranslation {
  isDirty: boolean;
}

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-translatable',
  templateUrl: './translatable.component.html',
  styleUrls: ['./translatable.component.scss']
})
export class TranslatableComponent implements OnInit {

  @Input() public translatable: Translatable;
  @Input() public required = true;
  @Output() public translatableChange: EventEmitter<Translatable> = new EventEmitter;

  public selectedEl: Translatable;
  public allowedLangCodes: Observable<string[]>;
  public translations: TranslationsWithDirtyState[];

  @ViewChild(WidgetChooseItemModalComponent) private _chooseItemModalComponent: WidgetChooseItemModalComponent;

  constructor(public adminTranslatableService: AdminTranslatableService) {

  }

  public ngOnInit(): void {
    this.allowedLangCodes = this.adminTranslatableService.findAllowedLangCodes();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param translation
   */
  public markAsdirty(translation: TranslationsWithDirtyState): void {
    translation.isDirty = true;
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param translation
   * @returns
   */
  public async clickSaveTranslation(translation: TranslationsWithDirtyState): Promise<void> {
    try {
      translation.isDirty = false;
      const saved = await this.adminTranslatableService.addTranslation(this.selectedEl.id, translation).toPromise();
      translation.id = saved.id;
    } catch (e) {
      translation.isDirty = true;
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param translation
   */
  public clickDeleteTranslation(translation: TranslationsWithDirtyState): void {
    if (translation.id) {
      if (confirm('Are you sure?')) {
        this.adminTranslatableService.deleteTranslation(translation.id).subscribe(() => {
          this.translations = this.translations.filter(current => current !== current);
        });
      }
    } else {
      this.translations = this.translations.filter(current => current !== current);
    }
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param el
   */
  public async onSelection(el: Translatable): Promise<void> {
    this.selectedEl = el;
    this.translations = el.id
      ? (await this.adminTranslatableService.findTranslations(el.id).toPromise()).map(current => ({
        ...current,
        isDirty: false
      }))
      : [];
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   */
  public clickAddTranslation(): void {
    this.translations.push({
      id: null,
      langCode: 'en',
      value: '',
      isDirty: true
    });
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @param translatable
   */
  public onChoosen(translatable: Translatable) {
    this.translatable = translatable;
    this._chooseItemModalComponent.hide();
    this.translatableChange.emit(translatable);
  }
}
