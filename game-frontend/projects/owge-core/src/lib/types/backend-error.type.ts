
/**
 * Represents an error thrown by the backend
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface BackendError {
    developerHint: string;
    exceptionType: string;
    extra: { [key: string]: any };
    message: string;
    reporterAsString: string;
}
