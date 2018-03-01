import { Observable } from 'rxjs/Rx';
import { ResourceManagerService } from '../../app/service/resource-manager.service';
import { RequirementPojo } from '../../app/shared-pojo/requirement.pojo';

describe('RequirementPojo...', () => {
    let instance: RequirementPojo;

    beforeEach(() => {
        instance = new RequirementPojo();
    });

    it('creates a new instance', () => {
        expect(instance).toEqual(jasmine.any(RequirementPojo));
    });

    it('when subscribing to resources current status, should stop subscription before start', () => {
        spyOn(instance, 'stopDynamicRunnable').and.stub();
        const serviceInstance = new ResourceManagerService();
        serviceInstance['_currentPrimaryResourceFloor'] = (<any>Observable).of(4);
        instance.startDynamicRunnable(serviceInstance);
        expect(instance.stopDynamicRunnable).toHaveBeenCalledTimes(1);
    });
});
