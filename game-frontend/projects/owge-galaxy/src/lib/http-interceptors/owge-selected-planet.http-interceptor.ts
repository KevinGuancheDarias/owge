import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { PlanetService } from '../services/planet.service';
import { take, flatMap } from 'rxjs/operators';
import { Planet } from '@owge/universe';

@Injectable()
export class OwgeSelectedPlanetHttpInterceptor implements HttpInterceptor {

    public constructor(private planetService: PlanetService) { }

    public intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        if (!req.url.startsWith('./assets') && !req.url.includes('/websocket-sync')) {
            return this.planetService.findCurrentPlanet()
                .pipe(
                    take(1),
                    flatMap(planet => this.doSetSelectedPlanet(planet, req, next))
                );
        } else {
            return next.handle(req);
        }
    }

    private doSetSelectedPlanet(planet: Planet, req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        const newRequest: HttpRequest<any> = req.clone({
            headers: req.headers.set('X-OWGE-Selected-Planet', String(planet.id))
        });
        return next.handle(newRequest);
    }
}
