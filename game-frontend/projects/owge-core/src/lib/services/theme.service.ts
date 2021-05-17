import { Injectable } from '@angular/core';
import { Observable, of, ReplaySubject, Subject } from 'rxjs';
import { ProgrammingError } from '../errors/programming.error';

@Injectable()
export class ThemeService {

    private supportedThemes = ['classic', 'neon'];
    private currentThemeSubject: Subject<string> = new ReplaySubject(1);
    private currentTheme: string;
    private defaultTheme = 'classic';

    public get currentTheme$(): Observable<string> {
        return this.currentThemeSubject.asObservable();
    }

    public findAll(): Observable<string[]> {
        return of(this.supportedThemes);
    }

    public useDefaultTheme(): void {
        this.useTheme(this.defaultTheme);
    }

    public useTheme(themeName: string): void {
        if(!this.supportedThemes.includes(themeName)) {
            throw new ProgrammingError(`Unknown theme: ${themeName}`);
        }
        const bodyClasses: DOMTokenList = window.document.body.classList;
        bodyClasses.remove(this.resolveThemeName(this.currentTheme));
        bodyClasses.add(this.resolveThemeName(themeName));
        bodyClasses.add('owge-theme');
        this.currentTheme = themeName;
        this.currentThemeSubject.next(themeName);
    }

    private resolveThemeName(inputTheme: string): string {
        return `owge-theme-${inputTheme}`;
    }
}
