package com.kevinguanchedarias.owgejava.business.timespecial;

import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.business.WithUnlockableBo;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheEvictByTag;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UnlockableTimeSpecialService implements WithUnlockableBo<Integer, TimeSpecial, TimeSpecialDto> {
    private final UnlockedRelationBo unlockedRelationBo;

    /**
     * Evicts the cached "time specials with current status" list for the given user.
     * <p>
     * That list ({@link com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo#findByUserWithCurrentStatus})
     * is derived from the unlocked relations, but its cache tag is only invalidated when an
     * {@link ActiveTimeSpecial} row changes. Unlock/lock changes driven by the requirement engine
     * (e.g. HAVE_SPECIAL_LOCATION on conquest) only touch {@code unlocked_relation}, so the cache
     * must be evicted explicitly here or the available list goes stale.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @TaggableCacheEvictByTag(tags = ActiveTimeSpecial.ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG + ":#userId")
    public void evictByUserCache(Integer userId) {
        // Eviction is handled by the @TaggableCacheEvictByTag aspect
    }

    @Override
    public Class<TimeSpecialDto> getDtoClass() {
        return TimeSpecialDto.class;
    }

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.TIME_SPECIAL;
    }

    @Override
    public UnlockedRelationBo getUnlockedRelationBo() {
        return unlockedRelationBo;
    }
}
