package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.ImprovementSource;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.Serial;
import java.util.List;

@Component
public class ObtainedUpgradeBo implements BaseBo<Long, ObtainedUpgrade, ObtainedUpgradeDto>, ImprovementSource {
    public static final String OBTAINED_UPGRADE_CACHE_TAG = "obtained_upgrade";
    public static final String OBTANED_UPGRADE_CHANGE_EVENT = "obtained_upgrades_change";

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
        socketIoService.sendMessage(userId, OBTANED_UPGRADE_CHANGE_EVENT, () -> toDto(obtainedUpgradeRepository.findByUserId(userId)));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<ObtainedUpgrade> findByUpgrade(Upgrade upgrade) {
        return obtainedUpgradeRepository.findByUpgrade(upgrade);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.interfaces.ImprovementSource#
     * calculateImprovement()
     */
    @Override
    public GroupedImprovement calculateImprovement(UserStorage user) {
        return new GroupedImprovement().add(obtainedUpgradeRepository.findByUserId(user.getId()).stream()
                .map(current -> improvementBo.multiplyValues(current.getUpgrade().getImprovement(), current.getLevel()))
                .toList());
    }
}
