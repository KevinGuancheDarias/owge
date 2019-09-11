import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { CoreModule, OwgeUserModule, SessionService, JwtTokenUtil } from '@owge/core';
import { OwgeWidgetsModule } from '@owge/widgets';
import { OwgeUniverseModule } from '@owge/universe';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { environment } from '../environments/environment';
import { LoginComponent } from './components/login/login.component';
import { IndexComponent } from './components/index/index.component';
import { AdminLoginService } from './services/admin-login.service';
import { AdminUserStore } from './store/admin-user.store';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    IndexComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    CoreModule.forRoot({
      url: environment.accountUrl,
      loginEndpoint: environment.loginEndpoint,
      loginDomain: environment.loginDomain,
      loginClientId: environment.loginClientId,
      loginClientSecret: environment.loginClientSecret
    }),
    OwgeUserModule,
    OwgeUniverseModule.forRoot(),
    OwgeWidgetsModule,
  ],
  providers: [
    AdminLoginService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  public constructor(sessionService: SessionService, adminUserStore: AdminUserStore, adminLoginService: AdminLoginService) {
    sessionService.initStore();
    const key = AdminLoginService.SESSION_STORAGE_KEY;
    const rawToken: string = sessionStorage.getItem(key);
    const token = JwtTokenUtil.findTokenIfNotExpired(
      rawToken,
      () => sessionStorage.removeItem(key)
    );
    if (token) {
      adminLoginService.setToken(rawToken);
    }
  }
}
