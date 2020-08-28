
/**
 * Represents a galaxy
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface Galaxy {
    id: number;
    name: string;
    sectors: number;
    quadrants: number;
    numPlanets?: number;
    orderNumber: number;
}
