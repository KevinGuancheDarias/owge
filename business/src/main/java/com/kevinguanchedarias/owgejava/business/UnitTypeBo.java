package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.checker.EntityCanDoMissionChecker;
import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementChangeEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.FactionUnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.responses.UnitTypeResponse;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serial;
import java.util.List;

@Service
@AllArgsConstructor
public class UnitTypeBo implements WithNameBo<Integer, UnitType, UnitTypeDto> {
    public static final String UNIT_TYPE_CACHE_TAG = "unit_type";
    public static final String UNIT_TYPE_CHANGE = "unit_type_change";

    @Serial
    private static final long serialVersionUID = 1064115662505668879L;

    private final UnitTypeRepository unitTypeRepository;
    private final ImprovementBo improvementBo;
    private final UserStorageRepository userStorageRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final transient SocketIoService socketIoService;
    private final transient FactionUnitTypeRepository factionUnitTypeRepository;
    private final transient EntityCanDoMissionChecker entityCanDoMissionChecker;
    private final DtoUtilService dtoUtilService;

    @Override
    public JpaRepository<UnitType, Integer> getRepository() {
        return unitTypeRepository;
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

    @PostConstruct
    public void init() {
        improvementBo.addChangeListener(ImprovementChangeEnum.UNIT_IMPROVEMENTS, (userId, improvement) -> {
            if (improvement.getUnitTypesUpgrades().stream()
                    .anyMatch(current -> ImprovementTypeEnum.AMOUNT.name().equals(current.getType()))) {
                emitUserChange(userId);
            }
        });
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
     * <b>NOTICE: It will process all the obtained upgrades and obtained units to
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
            current.getSpeedImpactGroup().setRequirementGroups(null);
            var unitTypeResponse = dtoUtilService.dtoFromEntity(UnitTypeResponse.class, current);
            var user = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, userId);
            unitTypeResponse.setComputedMaxCount(findUniTypeLimitByUser(user, current));
            if (hasMaxCount(user.getFaction(), current)) {
                unitTypeResponse.setUserBuilt(obtainedUnitRepository.countByUserAndUnitType(user, current));
            }
            unitTypeResponse.setUsed(isUsed(current.getId()));
            return unitTypeResponse;
        }).toList();
    }

    public void emitUserChange(Integer userId) {
        socketIoService.sendMessage(userId, UNIT_TYPE_CHANGE, () -> findUnitTypesWithUserInfo(userId));
    }

    /**
     * Checks if the specified count would be over the expected count
     *
     * @throws SgtBackendInvalidInputException When reached
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void checkWouldReachUnitTypeLimit(UserStorage user, Integer typeId, Long count) {
        if (wouldReachUnitTypeLimit(user, typeId, count)) {
            throw new SgtBackendInvalidInputException(
                    "Nice try to buy over your possibilities!!!, try outside of Spain!");
        }
    }

    private boolean wouldReachUnitTypeLimit(UserStorage user, Integer typeId, Long count) {
        var faction = user.getFaction();
        var type = findById(typeId);
        var targetToCountTo = findMaxShareCountRoot(type);
        var userCount = ObjectUtils.firstNonNull(countUnitsByUserAndUnitType(user, targetToCountTo), 0L) + count;
        return hasMaxCount(faction, targetToCountTo)
                && userCount > findUniTypeLimitByUser(user, targetToCountTo.getId());
    }

    private UnitType findMaxShareCountRoot(UnitType type) {
        return type.getShareMaxCount() == null ? type : findMaxShareCountRoot(type.getShareMaxCount());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    private long countUnitsByUserAndUnitType(UserStorage user, UnitType type) {
        return ObjectUtils.firstNonNull(obtainedUnitRepository.countByUserAndUnitType(user, type), 0L)
                + ObjectUtils.firstNonNull(obtainedUnitRepository.countByUserAndSharedCountUnitType(user, type), 0L);
    }
}
