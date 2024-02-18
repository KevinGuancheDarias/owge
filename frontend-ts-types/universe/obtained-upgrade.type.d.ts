import { Upgrade } from './upgrade.type';

/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 * @export
 */
export interface ObtainedUpgrade<R = any> {
    id: number;
    level: number;
    available: boolean;
    requirements: R;
    upgrade: Upgrade;
}
