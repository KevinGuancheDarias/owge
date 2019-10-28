/**
 * Represents a ranking entry
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @interface RankingEntry
 */
export interface RankingEntry {
    position: number;
    points: number;
    userId: number;
    username: string;
    allianceId: number;
    allianceName: string;
}
