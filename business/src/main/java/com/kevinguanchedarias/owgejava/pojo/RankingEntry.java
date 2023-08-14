/**
 *
 */
package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.dto.CommonDtoWithImageStore;
import com.kevinguanchedarias.owgejava.entity.Faction;

/**
 * Represents a ranking entry
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
public record RankingEntry(
        Number position,
        Number points,
        Number userId,
        String username,
        Number allianceId,
        String allianceName,
        CommonDtoWithImageStore<Integer, Faction> faction
) {
}
