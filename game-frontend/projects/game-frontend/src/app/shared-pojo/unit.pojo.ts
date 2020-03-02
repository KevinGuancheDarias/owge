import { MEDIA_ROUTES, LoggerHelper } from '@owge/core';

import { RequirementPojo } from './requirement.pojo';

/**
 * Represents a Unit as sent by backend
 *
 * @deprecated As of 0.9.0 it's better to use ng:/OwgeUniverse/types/unit.type.ts
 * @todo Complete the missing params, after making the backend DTO
 * @author Kevin Guanche Darias
 */
export class UnitPojo {

    private static readonly _LOG: LoggerHelper = new LoggerHelper(UnitPojo.name);

    public id: number;
    public name: string;
    public image?: string;
    public points?: number;
    public description?: string;
    public time?: number;
    public primaryResource?: number;
    public secondaryResource?: number;
    public improvement?: any;
    public energy?: number;
    public attack?: number;
    public health?: number;
    public shield?: number;
    public charge?: number;
    public isUnique?: boolean;
    public typeId: number;
    public typeName?: string;
    public requirements?: RequirementPojo;

    /**
     * Returns the full path to the unit image
     *
     * @deprecated As of 0.8.1 use the dynamicImage pipe instead
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @static
     * @param {UnitPojo} unit
     * @returns {string}
     * @memberof UnitPojo
     */
    public static findImagePath(unit?: UnitPojo): string {
        this._LOG.warnDeprecated('UnitPojo.findImagePath()', '0.8.1', 'ng://OwgeCore/pipes/dynamicImage');
        return MEDIA_ROUTES.IMAGES_ROOT + unit.image;
    }
}
