import { Directive, Input, OnDestroy, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';
import { Subscription } from 'rxjs';
import { ThemeService } from '../services/theme.service';

@Directive({
    selector: '[owgeCoreIfTheme]'
})
export class OwgeCoreIfThemeDirective implements OnInit, OnDestroy{
    #wantedTheme: string;
    #currentTheme: string;
    #subscription: Subscription;

    @Input()
    set owgeCoreIfTheme(theme: string) {
        this.#wantedTheme = theme;
        this.updateView();
    }

    constructor(
        private templateRef: TemplateRef<any>,
        private viewContainer: ViewContainerRef,
        private themeService: ThemeService
    ) {}

    public ngOnInit(): void {
        this.#subscription = this.themeService.currentTheme$.subscribe(theme => {
            this.#currentTheme = theme;
            this.updateView();
        });
    }

    public ngOnDestroy(): void {
        this.#subscription.unsubscribe();
    }


    private updateView() {
        if(this.#currentTheme === this.#wantedTheme) {
            this.viewContainer.createEmbeddedView(this.templateRef);
        } else {
            this.viewContainer.clear();
        }
    }
}
