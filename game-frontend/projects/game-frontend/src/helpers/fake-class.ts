import { Type } from '@angular/core';

/**
 * when using useValue in unit test, if object is not a class, its instance is not passed, use this empty class for that purpose
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 * @class FakeClass
 */
export class FakeClass {

    public static getInstance<T>(type: Type<T>): T {
        return <any>new FakeClass(type);
    }
    private constructor(private _fakedType: any) {
        // Can't initialize this class from outside
    }
}
