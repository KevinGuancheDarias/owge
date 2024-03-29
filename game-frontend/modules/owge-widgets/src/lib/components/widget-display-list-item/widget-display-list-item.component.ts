import {
  AfterContentInit, Component,





  ContentChildren, EventEmitter, Input,



  OnDestroy, OnInit,
  Output,



  QueryList,

  TemplateRef
} from '@angular/core';
import { ContentTransclusionUtil, OwgeContentDirective, ProgrammingError, ScreenDimensionsService, ThemeService } from '@owge/core';
import { Subscription } from 'rxjs';


/**
 * Widget to display an element <br>
 * <b>Can place child content at the following selectors: <b>
 * <ul>
 * <li>extra-section</li>
 * <li>extra-header</li>
 * <li>extra-description</li>
 * <li>action-buttons></li>
 * <li>image-container-prepend</li>
 * </ul>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Component({
  selector: 'owge-widgets-display-list-item',
  templateUrl: './widget-display-list-item.component.html',
  styleUrls: ['./widget-display-list-item.component.scss']
})
export class WidgetDisplayListItemComponent implements OnInit, OnDestroy, AfterContentInit {

  @Input() public image: string;
  @Input() public itemName: string;
  @Input() public itemDescription: string;
  @Input() public hasToDisplayCountdown = false;
  @Input() public countdownDate: Date;
  @Input() public countdownMillis: number;
  @Input() public desktopWidth = 767;
  @Input() public hideDesktopSections = [];
  @Input() public hideMobileSections = [];
  @Input() public classes: any = {};
  @Output() public timeOver: EventEmitter<void> = new EventEmitter();
  @ContentChildren(OwgeContentDirective) private _templatesList: QueryList<OwgeContentDirective>;

  public isDesktop: boolean;
  extraTitleTemplate: TemplateRef<any>;
  public extraSectionTemplate: TemplateRef<any>;
  public extraHeaderTemplate: TemplateRef<any>;
  public actionButtonsTemplate: TemplateRef<any>;
  public imageContainerPrependTemplate: TemplateRef<any>;
  public extraDescriptionTemplate: TemplateRef<any>;
  public currentTheme: string;

  private _sdsIdentifier: string;
  private _subscription: Subscription;
  private currentThemeSubscription: Subscription;

  public constructor(private _screenDimensionsService: ScreenDimensionsService, themeService: ThemeService) {
    this._sdsIdentifier = this._screenDimensionsService.generateIdentifier(this);
    this.currentThemeSubscription = themeService.currentTheme$.subscribe(theme => this.currentTheme = theme);
  }

  /**
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.8.1
   */
  public ngOnInit(): void {
    this._subscription = this._screenDimensionsService.hasMinWidth(this.desktopWidth, this._sdsIdentifier).subscribe(val => {
      this.isDesktop = val;
    });
    if (this.countdownDate && this.countdownMillis) {
      throw new ProgrammingError('You may specify countdownDate or countdownSeconds, but not both');
    }
  }

  public ngAfterContentInit() {
    this.extraTitleTemplate = ContentTransclusionUtil.findInList(this._templatesList, 'extra-title');
    this.extraSectionTemplate = ContentTransclusionUtil.findInList(this._templatesList, 'extra-section');
    this.extraHeaderTemplate = ContentTransclusionUtil.findInList(this._templatesList, 'extra-header');
    this.extraDescriptionTemplate = ContentTransclusionUtil.findInList(this._templatesList, 'extra-description');
    this.actionButtonsTemplate = ContentTransclusionUtil.findInList(this._templatesList, 'action-buttons');
    this.imageContainerPrependTemplate = ContentTransclusionUtil.findInList(this._templatesList, 'image-container-prepend');
  }

  public ngOnDestroy(): void {
    this._subscription.unsubscribe();
    this._screenDimensionsService.removeHandler(this._sdsIdentifier);
    this.currentThemeSubscription.unsubscribe();
  }

}
