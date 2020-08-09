import { HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';


/**
 * Options that can be defined in the HTTP request
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 */
export interface HttpOptions {
    headers?: HttpHeaders;
    observe?: 'body';
    params?: HttpParams | {
        [param: string]: string | string[];
    };
    reportProgress?: boolean;
    responseType?: 'json';
    withCredentials?: boolean;
    errorHandler?: (err: HttpErrorResponse | any, caught?: Observable<any>) => Observable<any>;
}
