import { Directive, Input, TemplateRef } from '@angular/core';

@Directive({
    selector: '[owgeCoreContent]',
})
export class OwgeContentDirective<C = any> {
    @Input() public select;

    public constructor(public templateRef: TemplateRef<C>) { }
}
