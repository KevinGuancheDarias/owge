import { Injectable } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { TRUSTED_DOMAINS } from '../../security';

@Injectable()
export class SanitizeService {

  constructor(private _domSanitizer: DomSanitizer) { }


  public isSafe(url: string): boolean {
    const urlObject: URL = new URL(url);
    return TRUSTED_DOMAINS.some(current => urlObject.hostname.endsWith(current));
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
