import { HttpErrorResponse } from '@angular/common/http';
import { ProgrammingError } from '../errors/programming.error';


/**
 * Has helper methods to handle http request or responses
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class HttpUtil {

    /**
     * Will translate the server error message to a single string
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param error
     * @returns
     */
    public static translateServerError(error: Response | HttpErrorResponse | any): string {
        try {
            let body: any;
            if (error instanceof HttpErrorResponse) {
                body = error.error;
            } else if (error instanceof Response) {
                body = error.json() || ''; // Should have a default server exception pojo
            } else if (error.exceptionType && error.message) {
                body = error;
            } else {
                throw new ProgrammingError('Unexpected value for error');
            }
            return body.message ? body.message : 'El servidor no respondió correctamente';
        } catch (e) {
            return 'El servidor no devolvió un JSON válido';
        }
    }
}
