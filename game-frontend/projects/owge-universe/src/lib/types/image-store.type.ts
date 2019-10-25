
/**
 * Represents a Backend <i>ImageStore</i>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
export interface ImageStore {
    id: number;
    checksum: string;
    filename: string;
    displayName: string;
    description: string;
    url: string;
}
