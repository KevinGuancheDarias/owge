import { RequirementPojo } from './requirement.pojo';
import { Upgrade } from '@owge/universe';

export class ObtainedUpgradePojo {
    public id: number;
    public level: number;
    public available: boolean;

    public requirements: RequirementPojo;
    public upgrade: Upgrade;
}
