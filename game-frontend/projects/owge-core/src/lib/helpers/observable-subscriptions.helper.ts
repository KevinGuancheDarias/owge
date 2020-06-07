import { Subscription } from 'rxjs';

/**
 * Eases the clean up of suscriptions
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export class ObservableSubscriptionsHelper {
    protected _subscriptions: Subscription[] = [];


    /**
     * Adds a subscription
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @param subscription
     */
    public add(...subscription: Subscription[]): void {
        this._subscriptions = this._subscriptions.concat(subscription);
    }

    /**
     * Unsubscribes all the added subscriptions
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public unsubscribeAll(): void {
        this._subscriptions.forEach(current => current.closed || current.unsubscribe());
    }
}
