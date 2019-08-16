import { Component, OnInit, ElementRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-version-information',
  templateUrl: './version-information.component.html',
  styleUrls: ['./version-information.component.less']
})
export class VersionInformationComponent implements OnInit {

  public htmlContent = 'Loading...';

  constructor(private _http: HttpClient, private _ref: ElementRef) { }

  ngOnInit() {
    this._http.get('assets/html/changelog.html', { responseType: 'text' }).subscribe(result => {
      this.htmlContent = result;
      setTimeout(() => {
        this._handleImages();
        this._handleLinks();
      }, 500);
    });
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
}
