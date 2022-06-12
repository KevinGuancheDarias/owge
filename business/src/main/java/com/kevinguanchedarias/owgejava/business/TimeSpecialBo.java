package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.TimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import com.kevinguanchedarias.owgejava.util.ValidationUtil;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Service
@AllArgsConstructor
public class TimeSpecialBo implements WithNameBo<Integer, TimeSpecial, TimeSpecialDto>,
        WithUnlockableBo<Integer, TimeSpecial, TimeSpecialDto> {
    public static final String TIME_SPECIAL_CACHE_TAG = "time_special";

    @Serial
    private static final long serialVersionUID = -2736277577264790898L;

    private final ImprovementBo improvementBo;
    private final ActiveTimeSpecialBo activeTimeSpecialBo;
    private final UnlockedRelationBo unlockedRelationBo;
    private final UserStorageBo userStorageBo;
    private final transient TimeSpecialRepository repository;
    private final transient TaggableCacheManager taggableCacheManager;
    private final ObjectRelationsRepository objectRelationsRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;
    private final RequirementInformationRepository requirementInformationRepository;

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
     */
    @Override
    public JpaRepository<TimeSpecial, Integer> getRepository() {
        return repository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return TIME_SPECIAL_CACHE_TAG;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
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

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.AbstractWithImageBo#save(com.
     * kevinguanchedarias.owgejava.entity.CommonEntityWithImageStore)
     */
    @Override
    public TimeSpecial save(TimeSpecial entity) {
        ValidationUtil.getInstance().requireNonEmptyString(entity.getName(), "name")
                .requirePositiveNumber(entity.getDuration(), "duration")
                .requirePositiveNumber(entity.getRechargeTime(), "rechargeTime");
        improvementBo.clearCacheEntriesIfRequired(entity, activeTimeSpecialBo);
        return WithNameBo.super.save(entity);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#save(java.util.List)
     */
    @Override
    public void save(List<TimeSpecial> entities) {
        improvementBo.clearCacheEntries(activeTimeSpecialBo);
        WithNameBo.super.save(entities);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.kevinguanchedarias.owgejava.business.BaseBo#delete(com.kevinguanchedarias
     * .owgejava.entity.EntityWithId)
     */
    @Override
    @Transactional
    public void delete(TimeSpecial entity) {
        activeTimeSpecialBo.deleteByTimeSpecial(entity);
        var or = objectRelationsRepository.findOneByObjectCodeAndReferenceId(ObjectEnum.TIME_SPECIAL.name(), entity.getId());
        unlockedRelationRepository.deleteByRelation(or);
        requirementInformationRepository.deleteByRelation(or);
        objectRelationsRepository.delete(or);
        WithNameBo.super.delete(entity);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.kevinguanchedarias.owgejava.business.BaseBo#delete(java.io.Serializable)
     */
    @Override
    @Transactional
    public void delete(Integer id) {
        WithNameBo.super.delete(id);
    }

    @Override
    public TimeSpecialDto toDto(TimeSpecial entity) {
        TimeSpecialDto timeSpecialDto = WithNameBo.super.toDto(entity);
        UserStorage loggedUser = userStorageBo.findLoggedIn();
        if (loggedUser != null) {
            timeSpecialDto.setActiveTimeSpecialDto(activeTimeSpecialBo
                    .toDto(activeTimeSpecialBo.findOneByTimeSpecial(entity.getId(), loggedUser.getId())));
        }
        return timeSpecialDto;
    }

}
