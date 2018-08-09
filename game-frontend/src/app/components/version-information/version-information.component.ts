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
        Array.from(this._getElement().querySelectorAll('img')).forEach(current => {
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
      }, 500);
    });
  }

  private _getElement(): HTMLElement {
    return this._ref.nativeElement;
  }
}
