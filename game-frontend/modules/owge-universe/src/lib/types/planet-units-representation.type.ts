/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 * @template T
 */
export interface PlanetsUnitsRepresentation<T> {
    planets: { [key: string]: T };
}
