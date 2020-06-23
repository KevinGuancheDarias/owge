import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { RequirementInformationWithTranslation } from '../../types/requirement-information-with-translation.type';


/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Component({
  selector: 'app-requirements-table',
  templateUrl: './requirements-table.component.html',
  styleUrls: ['./requirements-table.component.scss']
})
export class RequirementsTableComponent implements OnInit {

  @Input() public requirements: RequirementInformationWithTranslation[];
  @Output() public clickAdd: EventEmitter<void> = new EventEmitter;
  @Output() public clickDelete: EventEmitter<RequirementInformationWithTranslation> = new EventEmitter;

  constructor() { }

  ngOnInit(): void {
  }

}
