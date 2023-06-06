/**
 *
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.21
 * @export
 */
export interface Sponsor {
    name: string;
    description: string;
    type: 'COMPANY' | 'INDIVIDUAL';
    imageUrl?: string;
    url?: string;
}
