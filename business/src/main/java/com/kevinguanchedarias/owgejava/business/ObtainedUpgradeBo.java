package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ObtainedUpgradeBo implements BaseBo<Long, ObtainedUpgrade, ObtainedUpgradeDto>, ImprovementSource {
    public static final String OBTAINED_UPGRADE_CACHE_TAG = "obtained_upgrade";

    @Serial
    private static final long serialVersionUID = 2294363946431892708L;

    @Autowired
    private ObtainedUpgradeRepository obtainedUpgradeRepository;

    @Autowired
    private ImprovementBo improvementBo;

    @Autowired
    private transient SocketIoService socketIoService;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    @PostConstruct
    public void init() {
        improvementBo.addImprovementSource(this);
    }

    @Override
    public JpaRepository<ObtainedUpgrade, Long> getRepository() {
        return obtainedUpgradeRepository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return OBTAINED_UPGRADE_CACHE_TAG;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<ObtainedUpgradeDto> getDtoClass() {
        return ObtainedUpgradeDto.class;
    }

    /**
     * @param upgrade
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional
    public void deleteByUpgrade(Upgrade upgrade) {
        obtainedUpgradeRepository.deleteByUpgrade(upgrade);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void emitObtainedChange(Integer userId) {
        socketIoService.sendMessage(userId, "obtained_upgrades_change", () -> toDto(findByUser(userId)));
    }

    /**
     * Returns obtained upgrades by given user
     *
     * @param userId id of the user
     * @return
     * @author Kevin Guanche Darias
     */
    public List<ObtainedUpgrade> findByUser(Integer userId) {
        return obtainedUpgradeRepository.findByUserIdId(userId);
    }

    public ObtainedUpgrade findByUserAndUpgrade(Integer userId, Integer upgradeId) {
        return obtainedUpgradeRepository.findOneByUserIdIdAndUpgradeId(userId, upgradeId);
    }

    /**
     * @param upgrade
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<ObtainedUpgrade> findByUpgrade(Upgrade upgrade) {
        return obtainedUpgradeRepository.findByUpgrade(upgrade);
    }

    /**
     * Does user has the given upgrade obtained?
     *
     * @param userId    id of the user
     * @param upgradeId id of the asked upgrade
     * @return true if upgrade has been obtained
     * @author Kevin Guanche Darias
     */
    public boolean userHasUpgrade(Integer userId, Integer upgradeId) {
        return findUserObtainedUpgrade(userId, upgradeId) != null;
    }

    /**
     * Find user's obtained upgrade
     *
     * @param userId
     * @param upgradeId
     * @author Kevin Guanche Darias
     */
    public ObtainedUpgrade findUserObtainedUpgrade(Integer userId, Integer upgradeId) {
        return obtainedUpgradeRepository.findOneByUserIdIdAndUpgradeId(userId, upgradeId);
    }

    /**
     * Returns the total sum of the value for the specified improvement type for
     * user obtained upgrades
     *
     * @param user
     * @param type The expected type
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Long sumUnitTypeImprovementByUserAndImprovementType(UserStorage user, ImprovementTypeEnum type) {
        return ObjectUtils.firstNonNull(
                obtainedUpgradeRepository.sumByUserAndImprovementUnitTypeImprovementType(user, type.name()), 0L);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.interfaces.ImprovementSource#
     * calculateImprovement()
     */
    @Override
    public GroupedImprovement calculateImprovement(UserStorage user) {
        return new GroupedImprovement().add(findByUser(user.getId()).stream()
                .map(current -> improvementBo.multiplyValues(current.getUpgrade().getImprovement(), current.getLevel()))
                .collect(Collectors.toList()));
    }

}
