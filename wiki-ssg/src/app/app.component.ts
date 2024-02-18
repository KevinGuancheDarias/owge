import {Component} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {toSignal} from '@angular/core/rxjs-interop';
import {EnvironmentVariableService} from './services/environment-variable.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.sass'
})
export class AppComponent {
  title = 'owge-wiki-ssg';

  backendUrl = EnvironmentVariableService.getOrFail('OWGE_BACKEND_URL');
  serverMessage = toSignal(this.httpClient.get<string>(`${this.backendUrl}/open/clock`), {initialValue: 'Not loaded'});

  constructor(private httpClient: HttpClient) { }


}
