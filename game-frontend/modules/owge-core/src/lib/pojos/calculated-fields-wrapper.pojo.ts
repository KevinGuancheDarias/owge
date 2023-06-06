import { ProgrammingError } from '../errors/programming.error';

/**
 * Adds properties to backend types that are calculated in the frontend (usually used in the view) <br>
 * <b>Use case examples:</b><br>
 * <ul>
 * <li>Calculate dates</li>
 * <li>Computed states</li>
 * </ul>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export class CalculatedFieldsWrapper<T> {
    protected _calculatedFields: Map<keyof T, any> = new Map();

    /**
     * Creates an instance of CalculatedFieldsWrapper.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @param  element While readonly can be mutated (doesn't copy)
     */
    public constructor(public readonly element: T) { }


    /**
     * Adds a calculated field value
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param field Field to add (must exists in the element)
     * @param value Calculated value
     * @returns this
     */
    public addCalculatedField(field: keyof T, value: any): this {
        if (typeof this.element[field] === 'undefined') {
            throw new ProgrammingError(
                `Element ${this.element.constructor.name} doesn't have field ${field},
                you can't calculate from something that doesn't exists`
            );
        } else {
            this._calculatedFields.set(field, value);
        }
        return this;
    }

    /**
     * Returns the calculated field
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     * @param field
     * @returns
     */
    public getCalculatedField(field: keyof T): any {
        return this._calculatedFields.get(field);
    }
}
