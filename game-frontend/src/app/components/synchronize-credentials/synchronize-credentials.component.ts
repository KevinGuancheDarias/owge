import { Component, OnInit } from '@angular/core';
import { Credentials } from '../../shared/types/credentials.type';
import { LoginSessionService } from '../../login-session/login-session.service';
import { Router } from '@angular/router';
import { ROUTES } from '../../config/config.pojo';

@Component({
  selector: 'app-synchronize-credentials',
  templateUrl: './synchronize-credentials.component.html',
  styleUrls: ['./synchronize-credentials.component.less']
})
export class SynchronizeCredentialsComponent implements OnInit {

  constructor(private _loginSessionService: LoginSessionService, private _router: Router) { }

  public ngOnInit() {
    if (this._isIframe()) {
      const parentUrl: URL = new URL(document.referrer);
      if (parentUrl.hostname !== '192.168.99.100' && parentUrl.hostname.endsWith('kevinguanchedarias.com')) {
        alert('Security vulnerability attemp, try next time with a lammer trick!!');
      } else {
        window.addEventListener('message', (e: MessageEvent) => {
          if (e.data) {
            const credentialsDetails: Credentials = JSON.parse(e.data);
            if (credentialsDetails.rawToken && credentialsDetails.selectedUniverse) {
              this._loginSessionService.setTokenPojo(credentialsDetails.rawToken);
              this._loginSessionService.setSelectedUniverse(credentialsDetails.selectedUniverse);
              window.parent.postMessage('OK', '*');
            }
          }
        })
          ;
      }
    } else {
      this._router.navigate([ROUTES.GAME_INDEX]);
    }
  }

  private _isIframe(): boolean {
    try {
      return window.self !== window.parent || window.self !== window.parent;
    } catch (e) {
      return true;
    }
  }

}
