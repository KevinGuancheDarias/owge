import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject ,  Observable } from 'rxjs';

interface LoadingPromise extends Promise<any> {
  completed?: boolean;
}

@Injectable()
export class LoadingService implements OnDestroy {

  private static readonly INTERVAL_TIME_IN_SECONDS = 3;

  private _loadingPromises: LoadingPromise[] = [];
  private _intervalId: number;
  private _oldLoadingValue = false;
  private _loadingState: BehaviorSubject<boolean> = new BehaviorSubject(false);
  private _loadingState$: Observable<boolean> = this._loadingState.asObservable();

  public constructor() {
    this._startInterval();
  }

  public ngOnDestroy() {
    clearInterval(this._intervalId);
  }


  /**
   * Returns the same Promise, so it's easier to use await on expect promise
   *
   * @example const result = await addPromise(myService.findResult().toPromise());
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {Promise<any>} promise promise to register
   * @returns {Promise<any>} the passed promise
   * @memberof LoadingService
   */
  public addPromise(promise: Promise<any>): Promise<any> {
    const currentLoadingPromise: LoadingPromise = promise;
    this._loadingPromises.push(currentLoadingPromise);
    currentLoadingPromise.completed = false;
    promise.then(() => {
      currentLoadingPromise.completed = true;
    }).catch(e => console.error(`Failed promise in ${this.constructor.name}`, e));
    this._startInterval();
    return promise;
  }

  public observeLoading(): Observable<boolean> {
    return this._loadingState$;
  }

  /**
   * Runs a function that returns a promise, and displays a loading while the promise is <b>not</b> resolved
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @template T
   * @param {() => Promise<T>} action
   * @returns {Promise<T>}
   * @memberof LoadingService
   */
  public async runWithLoading<T = any>(action: () => Promise<T>): Promise<T> {
    return await this.addPromise(action());
  }

  /**
   * Registers the interval, resets the time if already define, and checks if the loading status has changed <b>(ASAP!!!)</b>
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @memberof LoadingService
   */
  private _startInterval(): void {
    if (this._intervalId) {
      clearInterval(this._intervalId);
    }
    this._handleIfLoading();
    this._intervalId = window.setInterval(() => this._handleIfLoading(), LoadingService.INTERVAL_TIME_IN_SECONDS);
  }

  private _handleIfLoading(): void {
    const isLoading = this._isLoading();
    if (isLoading !== this._oldLoadingValue) {
      if (isLoading) {
        this._removeCompletedPromises();
      }
      this._loadingState.next(isLoading);
    }
    this._oldLoadingValue = isLoading;
  }

  private _removeCompletedPromises(): void {
    this._loadingPromises = this._loadingPromises.filter(current => !current.completed);
  }

  private _isLoading(): boolean {
    return this._loadingPromises.some(current => !current.completed);
  }
}
