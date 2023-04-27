/**
 *
 */
package com.kevinguanchedarias.owgejava.pojo;

/**
 * Represents a ranking entry
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
public record RankingEntry(Number position, Number points, Number userId, String username, Number allianceId,
                           String allianceName) {
}
