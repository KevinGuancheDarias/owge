import { Injectable } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { TRUSTED_DOMAINS } from '../../security';

@Injectable()
export class SanitizeService {

  constructor(private _domSanitizer: DomSanitizer) { }

  /**
   * Checks if the URL is valid <br>
   * <b>NOTICE:</b> Non string inputs such as null, are always considered NOT safe
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {string} url
   * @returns {boolean} true if valid
   * @since 0.3.1
   * @memberof SanitizeService
   */
  public isSafe(url: string): boolean {
    if (typeof url === 'string') {
      const urlObject: URL = new URL(url);
      return TRUSTED_DOMAINS.some(current => urlObject.hostname.endsWith(current));
    } else {
      return false;
    }
  }

  /**
   * Sanitizes an URL, checks that is has a <b>valid domain and if it does </b> tells angular to mark it as safe
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {string} url
   * @returns {(SafeUrl | string)}
   * @memberof SanitizeService
   */
  public sanitizeUrl(url: string): SafeUrl | string {
    if (this.isSafe(url)) {
      return this._domSanitizer.bypassSecurityTrustResourceUrl(url);
    } else {
      return url;
    }
  }
}
