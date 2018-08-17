import { ResourceManagerService } from './../service/resource-manager.service';
import { AutoUpdatedResources } from './../class/auto-updated-resources';
import { Subscription } from 'rxjs/Subscription';

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
}
