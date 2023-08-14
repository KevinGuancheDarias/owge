/**
 *
 */
package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.CommonDtoWithImageStore;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.RankingEntry;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@Service
@AllArgsConstructor
public class RankingBo {
    private final UserStorageRepository userStorageRepository;

    /**
     * Find all the ranking entries <br>
     * <b>NOTICE:</b> NOT using pagination
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    @TaggableCacheable(
            tags = UserStorage.USER_CACHE_TAG
    )
    public List<RankingEntry> findRanking() {
        var position = new AtomicInteger(1);
        return userStorageRepository.findAllByOrderByPointsDesc().stream().map(current -> {
            var alliance = current.getAlliance();
            var hasAlliance = alliance != null;
            var userFaction = current.getFaction();
            CommonDtoWithImageStore<Integer, Faction> faction = new CommonDtoWithImageStore<>();
            faction.setName(userFaction.getName());
            faction.setDescription(userFaction.getDescription());
            faction.setImage(userFaction.getImage().getId());
            faction.setImageUrl(userFaction.getImage().getUrl());

            var retVal = new RankingEntry(
                    position.get(),
                    current.getPoints(),
                    current.getId(),
                    current.getUsername(),
                    hasAlliance ? alliance.getId() : null,
                    hasAlliance ? alliance.getName() : null,
                    faction
            );
            position.incrementAndGet();
            return retVal;
        }).toList();
    }
}
