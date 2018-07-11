import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-version-information',
  templateUrl: './version-information.component.html',
  styleUrls: ['./version-information.component.less']
})
export class VersionInformationComponent implements OnInit {

  public htmlContent = 'Loading...';

  constructor(private _http: HttpClient) { }

  ngOnInit() {
    this._http.get('assets/html/changelog.html', { responseType: 'text' }).subscribe(result => this.htmlContent = result);
  }

}
