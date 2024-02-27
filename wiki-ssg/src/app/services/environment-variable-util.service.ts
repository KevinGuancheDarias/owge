import {isPlatformBrowser} from '@angular/common';
import {Inject, Injectable, PLATFORM_ID} from '@angular/core';

@Injectable({providedIn: 'root'})
export class EnvironmentVariableUtilService {

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  getOrFail(name: string): string {
    if(!isPlatformBrowser(this.platformId)) {
      const value = process.env[name];
      if(!value) {
        throw new Error(`Missing env var ${name}`);
      }
      return value;
    } else {
      return '';
    }
  }
  doWith(name: string, action: (value: string) => {}, defaultValue?: string): void {
    const val = this.getOrFail(name) || defaultValue;
    if(val) {
      action(val);
    }
  }
}
