import {Component, Input, OnInit} from '@angular/core';
import {RuleWithUnitEntity} from '@owge/universe';

@Component({
  selector: 'owge-widgets-display-rule-table',
  templateUrl: './widget-display-rule-table.component.html',
  styleUrls: ['./widget-display-rule-table.component.scss']
})
export class WidgetDisplayRuleTableComponent implements OnInit {

  @Input() rules: RuleWithUnitEntity[];
  @Input() tableTranslations: string[];
  @Input() extraArgs: number[];
  @Input() extraArgsTransformations: string[];

  constructor() { }

  ngOnInit(): void {
  }

}
