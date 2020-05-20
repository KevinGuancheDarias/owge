import { LoggerHelper, ProgrammingError } from '@owge/core';

/**
 * Represents classes that are used to handle deliver_message websocket events
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @abstract
 * @class AbstractWebsocketApplicationHandler
 */
export abstract class AbstractWebsocketApplicationHandler {
    protected _eventsMap: { [eventName: string]: string } = {};

    protected _log: LoggerHelper = new LoggerHelper(this.constructor.name);


    /**
     * Actions to run when connected or reconnected to websocket
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     * @returns {Promise<void>}
     */
    public async workaroundSync(): Promise<void> {
        // TODO: Override to handle the sync call
    }

    /**
     * Returns the name of the public function used to handle an event
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} eventName Name of the event
     * @returns {string} string name of the local public function
     * @memberof AbstractWebsocketApplicationHandler
     */
    public getHandlerMethod(eventName: string): string {
        const handlerMethod = this._eventsMap[eventName];

        if (handlerMethod && typeof handlerMethod === 'string') {
            if (typeof this[handlerMethod] !== 'function') {
                throw new ProgrammingError(`${this.constructor.name}.${handlerMethod} is not a function`);
            }
            return handlerMethod;
        } else {
            return null;
        }
    }

    /**
     * Returns the map of events
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @returns {{ [eventName: string]: string; }} Key is the eventName, and the value is the method name
     * @memberof WebsocketApplicationHandler
     */
    public getEventsMap(): { [eventName: string]: string; } {
        return this._eventsMap;
    }

    /**
     * Executes the action
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param {string} eventName name of the event to execute
     * @param {*} content Content sent by the socket
     * @returns {Promise<any>} Promise resolved when the event has been solved inside the method handler
     * @throws {ProgrammingError} When the eventName doesn't have a hander in this websocket handler
     * @memberof WebsocketApplicationHandler
     */
    public async execute(eventName: string, content: any): Promise<any> {
        const functionName: string = this.getHandlerMethod(eventName);
        if (functionName && typeof this[functionName] === 'function') {
            this[functionName](content);
        } else {
            throw new ProgrammingError('Handler for ' + eventName + ' NOT found in ' + this.constructor.name);
        }
    }
}
