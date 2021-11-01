package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.FactionUnitType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.UnitTypesOverride;
import com.kevinguanchedarias.owgejava.repository.FactionRepository;
import com.kevinguanchedarias.owgejava.repository.FactionSpawnLocationRepository;
import com.kevinguanchedarias.owgejava.repository.FactionUnitTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

@Service
@Slf4j
public class FactionBo implements BaseBo<Integer, Faction, FactionDto> {
    @Serial
    private static final long serialVersionUID = -6735454832872729630L;

    private final FactionRepository repository;
    private final transient FactionUnitTypeRepository factionUnitTypeRepository;
    private final transient FactionSpawnLocationRepository factionSpawnLocationRepository;
    private final transient ConversionService conversionService;

    @Autowired
    @Lazy
    private UnitTypeBo unitTypeBo;

    public FactionBo(FactionRepository repository,
                     FactionUnitTypeRepository factionUnitTypeRepository,
                     FactionSpawnLocationRepository factionSpawnLocationRepository,
                     ConversionService conversionService
    ) {
        this.repository = repository;
        this.factionUnitTypeRepository = factionUnitTypeRepository;
        this.factionSpawnLocationRepository = factionSpawnLocationRepository;
        this.conversionService = conversionService;
    }

    @Override
    public JpaRepository<Faction, Integer> getRepository() {
        return repository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<FactionDto> getDtoClass() {
        return FactionDto.class;
    }

    /**
     * Returns the factions that are visible
     *
     * @param lazyFetch Fetch the proxies, or set to null
     * @author Kevin Guanche Darias
     */
    public List<Faction> findVisible(boolean lazyFetch) {
        List<Faction> retVal = repository.findByHiddenFalse();
        handleLazyFetch(lazyFetch, retVal);
        return retVal;
    }

    @Override
    public Faction save(Faction faction) {
        var customPrimaryGatherPercentage = faction.getCustomPrimaryGatherPercentage();
        var customSecondaryGatherPercentage = faction.getCustomSecondaryGatherPercentage();
        customPrimaryGatherPercentage = customPrimaryGatherPercentage != null ? customPrimaryGatherPercentage : 1;
        customSecondaryGatherPercentage = customSecondaryGatherPercentage != null ? customSecondaryGatherPercentage : 1;
        if ((customPrimaryGatherPercentage + customSecondaryGatherPercentage) > 0
                && (customPrimaryGatherPercentage + customSecondaryGatherPercentage) > 100) {
            throw new SgtBackendInvalidInputException(
                    "No, dear hacker, custom primary percentage plus secondary CAN'T be higher than 100");

        }
        return BaseBo.super.save(faction);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     */
    public Faction findByUser(Integer userId) {
        return repository.findOneByUsersId(userId);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    @Transactional
    public void saveOverrides(Integer factionId, List<UnitTypesOverride> overrides) {
        factionUnitTypeRepository.deleteByFactionId(factionId);
        overrides.forEach(override -> {
            var factionUnitType = new FactionUnitType();
            factionUnitType.setUnitType(unitTypeBo.getOne(override.getId()));
            factionUnitType.setFaction(getOne(factionId));
            factionUnitType.setMaxCount(override.getOverrideMaxCount());
            factionUnitTypeRepository.save(factionUnitType);
        });
    }

    private void handleLazyFetch(boolean lazyFetch, List<Faction> factions) {
        if (lazyFetch) {
            for (Faction current : factions) {
                Hibernate.initialize(current.getImprovement());
            }
        } else {
            for (Faction current : factions) {
                current.setImprovement(null);
            }
        }
    }

}
