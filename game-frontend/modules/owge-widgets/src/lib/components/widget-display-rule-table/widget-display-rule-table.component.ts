import {
  Component, ContentChild,
  Input,
  TemplateRef
} from '@angular/core';
import {RuleWithUnitEntity} from '@owge/universe';

@Component({
  selector: 'owge-widgets-display-rule-table',
  templateUrl: './widget-display-rule-table.component.html',
  styleUrls: ['./widget-display-rule-table.component.scss'],
})
export class WidgetDisplayRuleTableComponent {

  @Input() rules: RuleWithUnitEntity[];
  @Input() tableTranslations: string[];
  @Input() extraArgs: number[];
  @Input() extraArgsTransformations: string[];
  @Input() useArrayArgTransformations = true;
  @ContentChild('transformationBody', {static: false}) transformationBodyRef: TemplateRef<any>;

}
