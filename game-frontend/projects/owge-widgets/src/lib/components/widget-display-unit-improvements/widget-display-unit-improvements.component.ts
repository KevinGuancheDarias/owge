import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, ViewEncapsulation } from '@angular/core';
import { ImprovementUnitType, ProgrammingError, validImprovementType } from '@owge/core';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.17
 * @export
 */
@Component({
  selector: 'owge-widgets-display-unit-improvements',
  templateUrl: './widget-display-unit-improvements.component.html',
  styleUrls: ['./widget-display-unit-improvements.component.scss'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetDisplayUnitImprovementsComponent implements OnChanges {

  @Input() public unitTypeImprovements: ImprovementUnitType[];

  public constructor(private _chr: ChangeDetectorRef) { }

  public ngOnChanges(): void {
    this._chr.detectChanges();
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.17
   * @param type
   */
  public shouldUseImage(type: validImprovementType): boolean {
    return type !== 'AMOUNT';
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.17
   * @param type
   * @returns
   */
  public findImage(type: validImprovementType): string {
    switch (type) {
      case 'ATTACK':
        return 'sword.png';
      case 'DEFENSE':
        return 'hearth.png';
      case 'SHIELD':
        return 'shield.png';
      default:
        throw new ProgrammingError('Should never happend, at least in a happy perfect world');
    }
  }
}
