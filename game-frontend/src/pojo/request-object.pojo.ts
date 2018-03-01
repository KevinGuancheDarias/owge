import { ProgrammingError } from '../error/programming.error';
import { Config } from './../app/config/config.pojo';
import { RequestOptions, URLSearchParams } from '@angular/http';

export class RequestObject {
  public requestOptions: RequestOptions;

  /**
   * Handles url params and request options creation, useful to avoid repeating code <br />
   * The constructor will check:
   * <ul>
   *  <li>that urlSearchParams is an instance of URLSearchParams</li>
   *  <li>that requestOptions is not null, if it is, will create a new one with common headers
   * </ul>
   *
   * @author Kevin Guanche Darias
   */
  constructor(urlSearchParams: URLSearchParams, requestOptions: RequestOptions) {
    if (!requestOptions) {
      this.requestOptions = <RequestOptions>{};
      this.requestOptions.headers = Config.genCommonFormUrlencoded();
    } else {
      this.requestOptions = requestOptions;
    }

    if (urlSearchParams instanceof URLSearchParams) {
      this.requestOptions.search = urlSearchParams;
    }
  }
}
