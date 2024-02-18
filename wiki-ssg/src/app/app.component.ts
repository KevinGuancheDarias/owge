import {Component} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {toSignal} from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.sass'
})
export class AppComponent {
  title = 'owge-wiki-ssg';
  serverMessage = toSignal(this.httpClient.get<string>('http://localhost:8080/owgejava-game-rest/open/clock'), {initialValue: 'Not loaded'});

  constructor(private httpClient: HttpClient) { }


}
