import { ResourceManagerService } from './../service/resource-manager.service';
import { AutoUpdatedResources } from './../class/auto-updated-resources';
import { Subscription } from 'rxjs';

export class RequirementPojo {
    private _resources: AutoUpdatedResources;
    private _subscription: Subscription;

    public requiredPrimary: number;
    public requiredSecondary: number;
    public requiredTime: number;
    public requiredEnergy?: number;
    public runnable: boolean;

    /**
     * Fills the runnable property if possible
     *
     * @author Kevin Guanche Darias
     */
    public checkRunnable(resources: AutoUpdatedResources): void {
        const requiredEnergy: number = this.requiredEnergy
            ? this.requiredEnergy
            : 0;
        this.runnable = resources.currentPrimaryResource >= this.requiredPrimary
            && resources.currentSecondaryResource >= this.requiredSecondary
            && resources.availableEnergy() >= requiredEnergy;
    }

    public startDynamicRunnable(resourceManagerService: ResourceManagerService) {
        this.stopDynamicRunnable();
        this._resources = new AutoUpdatedResources(resourceManagerService);
        this._subscription = this._resources.resourcesAutoUpdate().subscribe(() => {
            this.checkRunnable(this._resources);
        });
    }

    public stopDynamicRunnable(): void {
        if (this._subscription) {
            this._subscription.unsubscribe();
        }
    }


    /**
     * returns the sum of base and percentage <br>
     * For a base of 10, and a percentage of 20, wold return 12
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param  base
     * @param  percentage
     * @returns
     */
    public handlePercentage(base: number, percentage: number): number {
        if (percentage == null) {
            return base;
        } else {
            return base + (base * (percentage / 100));
        }
    }

    /**
     * Returns the sustraction of base and percentage <br>
     * For a base of 10, and a percentage of 20, wold return 8
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param {number} base
     * @param {number} percentage
     * @returns {number}
     */
    public handleSustractionPercentage(base: number, percentage: number): number {
        return base * 2 + this.handlePercentage(-base, percentage);
    }
}
