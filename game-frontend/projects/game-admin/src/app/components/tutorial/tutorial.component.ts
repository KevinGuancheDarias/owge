import { Component, OnInit } from '@angular/core';
import { AdminTutorialService } from '../../services/admin-tutorial.service';
import { TutorialSection, TutorialSectionAvailableHtmlSymbol } from '@owge/universe';
import { TutorialSectionEntry } from 'projects/owge-universe/src/lib/types/tutorial-section-entry.type';
import { Observable } from 'rxjs';
import { AdminTutorialEntryService } from '../../services/admin-tutorial-entry.service';
import { TutorialEvent } from 'projects/owge-core/src/lib/types/tutorial-event.type';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-tutorial',
  templateUrl: './tutorial.component.html',
  styleUrls: ['./tutorial.component.scss']
})
export class TutorialComponent implements OnInit {

  public selectedEl: TutorialSectionEntry;
  public tutorialSections: TutorialSection[];
  public availableHtmlSymbols: TutorialSectionAvailableHtmlSymbol[];
  public validEvents: TutorialEvent[] = ['CLICK', 'ANY_KEY_OR_CLICK'];

  constructor(public adminTutorialEntryService: AdminTutorialEntryService, private _adminTutorialService: AdminTutorialService) {

  }

  public ngOnInit(): void {
    this._adminTutorialService.findAll().subscribe(val => this.tutorialSections = val);
    this._adminTutorialService.findHtmlSymbols().subscribe(val => this.availableHtmlSymbols = val);
  }

  /**
   *
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @since 0.9.0
   * @returns
   */
  public createNew(): TutorialSectionEntry {
    return <any>{
      event: 'CLICK'
    };
  }

}
