import { ProgrammingError } from 'error/programming.error';
import { Log, Level, Logger } from 'ng2-logger';

export class LoggerHelper {
    private _log: Logger<any>;
    public constructor(private _targetClass: string, ...level: Level[]) {
        this._log = Log.create(_targetClass, ...level);
    }

    public debug(...message: string[]): void {
        this._log.d(this._findCallerMethodName(), ...message);
    }

    public info(...message: string[]): void {
        this._log.i(this._findCallerMethodName(), ...message);
    }

    public warn(...message: string[]): void {
        this._log.w(this._findCallerMethodName(), ...message);
    }

    public error(...message: string[]): void {
        this._log.er(this._findCallerMethodName(), ...message);
    }

    private _findCallerMethodName(): string {
        let method;
        if (this._isChrome()) {
            const err = new Error();
            const stackLines: string[] = err.stack.split('\n');
            const lastClassCall: string = stackLines.filter(current => current.indexOf(this._targetClass) !== -1)[0];
            const methodLine: string[] = /at \w+\.(\w+)/.exec(lastClassCall);
            if (methodLine) {
                return methodLine[1];
            } else if (lastClassCall) {
                return 'constructor';
            } else {
                const lastCallbackCall: string = stackLines.slice(1).find(current => current.indexOf('LoggerHelper') === -1).trim();
                const invocationLine: string = lastCallbackCall.substr(lastCallbackCall.length - 6).split(':')[0];
                return '(callback) L' + invocationLine;
            }
        } else {
            method = 'notKnown';
        }
        return method;
    }
    private _isChrome(): boolean {
        return /Chrome/.test(navigator.userAgent) && /Google Inc/.test(navigator.vendor);
    }
}
