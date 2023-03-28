import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { Injectable } from '@angular/core';
import { CoreHttpService } from '../services/core-http.service';

/**
 * Adds the owge lang header
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
@Injectable()
export class LanguageHttpInterceptor implements HttpInterceptor {

    public constructor(private _translateService: TranslateService, private _coreHttpService: CoreHttpService) { }

    public intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        if (!req.url.startsWith('./assets')) {
            const newRequest: HttpRequest<any> = req.clone({
                headers: req.headers.set('X-Owge-Lang', this._translateService.getDefaultLang())
            });
            return next.handle(newRequest);
        } else {
            return next.handle(req);
        }

    }
}
