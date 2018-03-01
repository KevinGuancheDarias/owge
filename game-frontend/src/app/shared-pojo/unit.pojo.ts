import { RequirementPojo } from './requirement.pojo';
/**
 * Represents a Unit
 * 
 * @todo Complete the missing params, after making the backend DTO
 * @author Kevin Guanche Darias
 */
export class UnitPojo {
    public id: number;
    public name: string;
    public image: string;
    public points: number;
    public description: string;
    public time: number;
    public primaryResource: number;
    public secondaryResource: number;
    public improvement: any;
    public energy: number;
    public attack: number;
    public health: number;
    public shield: number;
    public charge: number;
    public requirements: RequirementPojo;
}