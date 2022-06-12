package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.checker.EntityCanDoMissionChecker;
import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionUnitType;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.FactionUnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import com.kevinguanchedarias.owgejava.responses.UnitTypeResponse;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.util.List;

@Component
public class UnitTypeBo implements WithNameBo<Integer, UnitType, UnitTypeDto> {
    public static final String UNIT_TYPE_CACHE_TAG = "unit_type";

    @Serial
    private static final long serialVersionUID = 1064115662505668879L;

    private static final String UNIT_TYPE_CHANGE = "unit_type_change";

    @Autowired
    private UnitTypeRepository unitTypeRepository;

    @Autowired
    private ImprovementBo improvementBo;

    @Autowired
    @Lazy
    private UnitMissionBo unitMissionBo;

    @Autowired
    private UserStorageBo userStorageBo;

    @Autowired
    private ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    private transient SocketIoService socketIoService;

    @Autowired
    private transient FactionUnitTypeRepository factionUnitTypeRepository;

    @Autowired
    @Lazy
    private ObtainedUnitBo obtainedUnitBo;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    @Autowired
    private transient EntityCanDoMissionChecker entityCanDoMissionChecker;

    @Override
    public JpaRepository<UnitType, Integer> getRepository() {
        return unitTypeRepository;
    }

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return UNIT_TYPE_CACHE_TAG;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<UnitTypeDto> getDtoClass() {
        return UnitTypeDto.class;
    }

    /**
     * Finds the max amount of a certain unit type the given user can have <br>
     * <b>NOTICE: It will proccess all the obtained upgrades and obtained units to
     * find the improvement</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Long findUniTypeLimitByUser(UserStorage user, Integer typeId) {
        UnitType type = findById(typeId);
        return findUniTypeLimitByUser(user, type);
    }

    /**
     * Finds the max amount of a certain unit type the given user can have <br>
     * <b>NOTICE: It will proccess all the obtained upgrades and obtained units to
     * find the improvement</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    public Long findUniTypeLimitByUser(UserStorage user, UnitType type) {
        var retVal = 0L;
        var faction = user.getFaction();
        if (hasMaxCount(faction, type)) {
            var groupedImprovement = improvementBo.findUserImprovement(user);
            retVal = (long) Math.floor(improvementBo.computeImprovementValue(findMaxCount(faction, type),
                    groupedImprovement.findUnitTypeImprovement(ImprovementTypeEnum.AMOUNT, type)));
        }
        return retVal;
    }

    public long findMaxCount(Faction faction, UnitType type) {
        return factionUnitTypeRepository.findOneByFactionAndUnitType(faction, type)
                .map(FactionUnitType::getMaxCount)
                .orElse(type.getMaxCount());
    }

    public boolean hasMaxCount(Faction faction, UnitType type) {
        return factionUnitTypeRepository.findOneByFactionAndUnitType(faction, type)
                .map(FactionUnitType::getMaxCount)
                .filter(maxCount -> maxCount > 0L)
                .isPresent()
                || type.hasMaxCount();
    }

    public boolean canDoMission(UserStorage user, Planet targetPlanet, List<UnitType> unitTypes, MissionType type) {
        return unitTypes.stream().allMatch(current -> entityCanDoMissionChecker.canDoMission(user, targetPlanet, current, type));
    }

    /**
     * Is used boolean.
     *
     * @param id the id
     * @return the boolean
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    public boolean isUsed(Integer id) {
        return unitTypeRepository.existsByUnitsTypeId(id);
    }

    public List<UnitTypeResponse> findUnitTypesWithUserInfo(Integer userId) {
        return findAll().stream().map(current -> {
            var unitTypeResponse = new UnitTypeResponse();
            current.getSpeedImpactGroup().setRequirementGroups(null);
            unitTypeResponse.dtoFromEntity(current);
            var user = userStorageBo.findById(userId);
            unitTypeResponse.setComputedMaxCount(findUniTypeLimitByUser(user, current));
            if (hasMaxCount(user.getFaction(), current)) {
                unitTypeResponse.setUserBuilt(obtainedUnitBo.countByUserAndUnitType(user, current));
            }
            unitTypeResponse.setUsed(isUsed(current.getId()));
            return unitTypeResponse;
        }).toList();
    }

    public void emitUserChange(Integer userId) {
        socketIoService.sendMessage(userId, UNIT_TYPE_CHANGE, () -> findUnitTypesWithUserInfo(userId));
    }
}
