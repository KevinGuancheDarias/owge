import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { skip, filter } from 'rxjs/operators';

import { LoggerHelper, ResourcesEnum } from '@owge/core';
import { UserWithFaction } from '@owge/faction';
import { UserStorage } from '../storages/user.storage';
import { Improvement } from '../types/improvement.type';
import { UserWithImprovements } from '../types/user-with-improvements.type';

/**
 * Thi service contains the logged in user resources <br />
 * It keeps the interface synced (adding resources each second) <br />
 *
 *
 * @author Kevin Guanche Darias
 */
@Injectable()
export class ResourceManagerService {
    public get currentPrimaryResource(): BehaviorSubject<number> {
        return this._currentPrimaryResourceFloor;
    }

    public get currentSecondaryResource(): BehaviorSubject<number> {
        return this._currentSecondaryResourceFloor;
    }

    public get currentEnergy(): Observable<number> {
        return this._currentEnergyFloor.asObservable();
    }

    public get currentMaxEnergy(): BehaviorSubject<number> {
        return this._currentMaxEnergyFloor;
    }

    private _intervalId: number;
    private _currentUser: UserWithFaction;

    private _currentPrimaryResource: number;
    private _currentPrimaryResourcePerSecond: number;
    private _currentSecondaryResource: number;
    private _currentSecondaryResourcePerSecond: number;
    private _currentConsumedEnergy: number;
    private _currentMaxEnergy: number;
    private _lastDate: Date;
    private _currentUserImprovementsSubscription: Subscription;
    private _log: LoggerHelper = new LoggerHelper(this.constructor.name);

    private _currentPrimaryResourceFloor: BehaviorSubject<number> = new BehaviorSubject(0);
    private _currentSecondaryResourceFloor: BehaviorSubject<number> = new BehaviorSubject(0);
    private _currentEnergyFloor: BehaviorSubject<number> = new BehaviorSubject(0);
    private _currentMaxEnergyFloor: BehaviorSubject<number> = new BehaviorSubject(0);

    constructor(private _userStore: UserStorage<UserWithFaction>) {
        _userStore.currentUser.pipe(
            filter(user => user && user.primaryResource && !!user.secondaryResource)
        ).subscribe(user => {
            this._setResources(user);
            this._currentUser = user;
            this.startHandling();
        });
    }

    /**
     *
     * @param user
     * @author Kevin Guanche Darias
     */
    public startHandling() {
        this.stopHandling();
        this._lastDate = new Date();
        this._intervalId = window.setInterval(() => {
            if (this._currentUser) {
                const currentDate: Date = new Date();

                this._currentPrimaryResource = this._calculateSum(
                    currentDate,
                    this._lastDate,
                    this._currentPrimaryResourcePerSecond,
                    this._currentPrimaryResource
                );
                this._currentPrimaryResourceFloor.next(Math.floor(this._currentPrimaryResource));

                this._currentSecondaryResource = this._calculateSum(
                    currentDate,
                    this._lastDate,
                    this._currentSecondaryResourcePerSecond,
                    this._currentSecondaryResource
                );
                this._currentSecondaryResourceFloor.next(Math.floor(this._currentSecondaryResource));

                this._lastDate = currentDate;
            }
        }, 1000);
    }

    public stopHandling() {
        if (this._intervalId) {
            window.clearInterval(this._intervalId);
            this._intervalId = null;
        }
    }

    /**
     * Will add value to resource count
     *
     * @param resourceType Type of resource (primary,secondary ...)
     * @param value Number to add
     *
     * @author Kevin Guanche Darias
     */
    public addResources(resourceType: ResourcesEnum, value: number): void {
        switch (resourceType) {
            case ResourcesEnum.PRIMARY:
                this._currentPrimaryResource += value;
                this._currentPrimaryResource = this._currentPrimaryResource < 0 ? 0 : this._currentPrimaryResource;
                break;
            case ResourcesEnum.SECONDARY:
                this._currentSecondaryResource += value;
                this._currentSecondaryResource = this._currentSecondaryResource < 0 ? 0 : this._currentSecondaryResource;
                break;
            case ResourcesEnum.CONSUMED_ENERGY:
                this._currentConsumedEnergy += value;
                this._currentEnergyFloor.next(Math.floor(this._currentConsumedEnergy));
                break;
            default:
                throw new Error('Unexpected type ' + resourceType);
        }
    }

    /**
     * Will substract value to resource count
     *
     * @param resourceType Type of resource (primary,secondary ...)
     * @param value Number to substract
     *
     * @author Kevin Guanche Darias
     */
    public minusResources(resourceType: ResourcesEnum, value: number): void {
        this.addResources(resourceType, value * -1);
    }

    private _setResources(user: UserWithFaction & UserWithImprovements) {
        this._setPrimaryValue(user.primaryResource);
        this._setSecondaryValue(user.secondaryResource);

        this._definePerSecondVariables(user, user.improvements);
        if (this._currentUserImprovementsSubscription) {
            this._currentUserImprovementsSubscription.unsubscribe();
        }
        this._currentUserImprovementsSubscription = this._userStore.currentUserImprovements.pipe(skip(1)).subscribe(improvement => {
            const oldPerSecond = this._findPerSecondVariables();
            this._definePerSecondVariables(user, improvement);
            const newPerSecond = this._findPerSecondVariables();
            this._log.debug('Recalculating per second values from old', oldPerSecond, 'to', newPerSecond);
        });


        this._currentConsumedEnergy = user.consumedEnergy;
        this._currentMaxEnergy = user.maxEnergy;

        this._currentEnergyFloor.next(Math.floor(this._currentConsumedEnergy));
        this._currentMaxEnergyFloor.next(Math.floor(this._currentMaxEnergy));
    }

    private _setPrimaryValue(value: number): void {
        this._currentPrimaryResource = value;
        this._currentPrimaryResourceFloor.next(Math.floor(this._currentPrimaryResource));
    }

    private _setSecondaryValue(value: number): void {
        this._currentSecondaryResource = value;
        this._currentSecondaryResourceFloor.next(Math.floor(this._currentSecondaryResource));
    }

    private _definePerSecondVariables(user: UserWithFaction, improvement: Improvement) {
        this._currentPrimaryResourcePerSecond = this._computeUserResourcePerSecond(
            user.faction.primaryResourceProduction,
            improvement && improvement.morePrimaryResourceProduction
        );
        this._currentSecondaryResourcePerSecond = this._computeUserResourcePerSecond(
            user.faction.secondaryResourceProduction,
            improvement && improvement.moreSecondaryResourceProduction
        );
        this._log.debug('maxEnergy', user.faction.initialEnergy + (user.faction.initialEnergy * (improvement.moreEnergyProduction / 100)));
    }

    /**
     * calculates the new value using the date diff and the value per second<br />
     * <b>WARNING!: To avoid float madness ensure that atleast there is a interval of 500ms</b>
     *
     * @see java://owge-backend/com.kevinguanchedarias.owgejava.business.UserStorageBo(private)#calculateSum()
     * @param present datetime representing now
     * @param past datetime represents the last time value was update
     * @param perSecondValue Value to increase each second
     * @param value current Value
     * @returns The new value for the given resource
     * @author Kevin Guanche Darias
     */
    private _calculateSum(present: Date, past: Date, perSecondValue: number, value: number): number {
        let retVal = value;
        const difference: number = (present.getTime() - past.getTime()) / 1000;

        retVal += (difference * perSecondValue);
        return retVal;
    }

    /**
       * Computes the resource per second that one faction resource has according to
       * the current user improvement <br>
     * As of 0.8.0 calculate resource production in the frontend too
       *
       * @param factionResource     The faction resource production
       * @param resourceImprovement The improvement resource production
       * @return the computed resource per second
       * @author Kevin Guanche Darias
     * @since 0.8.0
       */
    private _computeUserResourcePerSecond(factionResource: number, resourceImprovement: number): number {
        const nanSafeImprovement: number = isNaN(resourceImprovement) ? 0 : resourceImprovement;
        return factionResource + (factionResource * (nanSafeImprovement / 100));
    }

    private _findPerSecondVariables() {
        return {
            primary: this._currentPrimaryResourcePerSecond,
            secondary: this._currentSecondaryResourcePerSecond
        };
    }
}
