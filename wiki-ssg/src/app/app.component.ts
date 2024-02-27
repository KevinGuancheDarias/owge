import {Component} from '@angular/core';
import {RouterLink, RouterOutlet} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {toSignal} from '@angular/core/rxjs-interop';
import {EnvironmentVariableUtilService} from './services/environment-variable-util.service';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {TranslateModule} from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MatButtonModule, MatMenuModule, MatIconModule, TranslateModule, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.sass',
})
export class AppComponent {
  title = 'owge-wiki-ssg';

  backendUrl = this.environmentVariableService.getOrFail('OWGE_BACKEND_URL');
  serverMessage = toSignal(this.httpClient.get<string>(`${this.backendUrl}/open/clock`), {initialValue: 'Not loaded'});

  constructor(private httpClient: HttpClient, private environmentVariableService: EnvironmentVariableUtilService) { }
}
