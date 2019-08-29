import { Injectable } from '@angular/core';

/**
 * Displays something in an alert(), confirm(), etc, used because in the future those actions may be styled
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Injectable()
export class DisplayService {

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  message
     * @returns
     */
    public async error(message: string): Promise<void> {
        alert(message);
    }

    /**
     *
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  message
     * @returns
     */
    public async confirm(message: string): Promise<boolean> {
        return this.confirm(message);
    }
}
