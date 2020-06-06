import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

interface ClassInformation {
  className: string;
  i18nKey: string;
  i18nValue: Observable<string>;
}

@Component({
  selector: 'app-version-information',
  templateUrl: './version-information.component.html',
  styleUrls: ['./version-information.component.less']
})
export class VersionInformationComponent implements OnInit {

  public htmlContent = 'Loading...';
  public classesInformation: ClassInformation[] = [];

  @ViewChild('changelogContent', { read: ElementRef })
  private _ref: ElementRef;

  constructor(private _http: HttpClient, private _translateService: TranslateService) { }

  ngOnInit() {
    this._http.get('assets/html/changelog.html', { responseType: 'text' }).subscribe(result => {
      this.htmlContent = result;
      setTimeout(() => {
        this._handleImages();
        this._handleLinks();
        this._handleClasses();
        this._hideClassesByDefault();
      }, 0);
    });
  }

  public clickCheckbox(event: any, className: string): void {
    const selector = `.${className}`;
    const target: HTMLInputElement = event.target;
    if (target.checked) {
      this._toggleElementDisplay(selector, 'list-item');
    } else {
      this._toggleElementDisplay(selector, 'none');
    }
  }

  private _hideClassesByDefault() {
    this._findElements('[class]').forEach(el => el.style.display = 'none');
  }

  private _toggleElementDisplay(elSelector: string, display: 'list-item' | 'none'): void {
    this._findElements(elSelector).forEach(el => el.style.display = display);
  }

  private _getElement(): HTMLElement {
    return this._ref.nativeElement;
  }

  private _findElements<T extends HTMLElement = HTMLElement>(selector: string): T[] {
    return Array.from<T>(this._getElement().querySelectorAll<any>(selector));
  }

  private _handleLinks(): void {
    this._findElements<HTMLLinkElement>('a').forEach(current => current.target = '_blank');
  }

  private _handleImages(): void {
    this._findElements('img').forEach(current => {
      const parent: HTMLElement = current.parentElement;
      if (parent.tagName === 'LI') {
        parent.style.cursor = 'pointer';
        parent.onclick = () => {
          if (current.style.display !== 'none') {
            current.style.display = 'none';
          } else {
            current.style.display = 'block';
          }
        };
      }
    });
  }

  private _handleClasses(): void {
    const classesContainer: HTMLDivElement = this._getElement().querySelector('.changelog-classes');
    classesContainer.style.display = 'none';
    Array.from(classesContainer.querySelectorAll('span')).forEach(current => {
      const className = current.innerText;
      const i18nKey = `APP.VERSION_INFORMATION.CLASSES.${className.toUpperCase()}`;
      this.classesInformation.push({
        className,
        i18nKey,
        i18nValue: this._translateService.get(i18nKey)
      });
    });
  }
}
