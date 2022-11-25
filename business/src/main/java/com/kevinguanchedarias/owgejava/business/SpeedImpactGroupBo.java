package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.dto.SpeedImpactGroupDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
public class SpeedImpactGroupBo implements BaseBo<Integer, SpeedImpactGroup, SpeedImpactGroupDto> {
    public static final String SPEED_IMPACT_GROUP_CACHE_TAG = "speed_impact_group";

    @Serial
    private static final long serialVersionUID = 1367954885113224567L;

    @Autowired
    private SpeedImpactGroupRepository speedImpactGroupRepository;

    @Autowired
    private transient ObjectRelationToObjectRelationRepository objectRelationToObjectRelationRepository;

    @Autowired
    private RequirementBo requirementBo;

    @Autowired
    private ObjectRelationBo objectRelationBo;

    @Autowired
    private UnlockedRelationBo unlockedRelationBo;

    @Autowired
    private DtoUtilService dtoUtilService;

    @Autowired
    @Lazy
    private transient SpeedImpactGroupFinderBo speedImpactGroupFinderBo;

    @Autowired
    private RequirementGroupRepository requirementGroupRepository;

    @Override
    public JpaRepository<SpeedImpactGroup, Integer> getRepository() {
        return speedImpactGroupRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<SpeedImpactGroupDto> getDtoClass() {
        return SpeedImpactGroupDto.class;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Override
    @Transactional
    public SpeedImpactGroup save(SpeedImpactGroupDto speedImpactGroupDto) {
        SpeedImpactGroup saved = save(dtoUtilService.entityFromDto(SpeedImpactGroup.class, speedImpactGroupDto));
        speedImpactGroupDto.getRequirementsGroups().forEach(requirementGroupDto -> {
            ObjectRelationToObjectRelation currentGroup = new ObjectRelationToObjectRelation();
            RequirementGroup requirementGroup = requirementGroupRepository.save(new RequirementGroup());
            currentGroup.setMaster(
                    objectRelationBo.findObjectRelationOrCreate(ObjectEnum.SPEED_IMPACT_GROUP, saved.getId()));
            requirementGroupDto.getRequirements().forEach(requirementInformationDto -> {
                ObjectRelationDto objectRelationDto = new ObjectRelationDto();
                objectRelationDto.dtoFromEntity(requirementGroup.getRelation());
                requirementInformationDto.setRelation(objectRelationDto);
                requirementBo.addRequirementFromDto(requirementInformationDto);
            });
            currentGroup.setSlave(requirementGroupRepository.save(requirementGroup).getRelation());
            objectRelationToObjectRelationRepository.save(currentGroup);
        });
        return speedImpactGroupRepository.save(saved);

    }

    public SpeedImpactGroup save(SpeedImpactGroup speedImpactGroup) {
        SpeedImpactGroup alreadySaved = speedImpactGroup.getId() == null ? null : findById(speedImpactGroup.getId());
        SpeedImpactGroup target = alreadySaved == null ? new SpeedImpactGroup() : alreadySaved;

        target.setIsFixed(speedImpactGroup.getIsFixed());
        target.setMissionAttack(speedImpactGroup.getMissionAttack());
        target.setMissionConquest(speedImpactGroup.getMissionConquest());
        target.setMissionCounterattack(speedImpactGroup.getMissionCounterattack());
        target.setMissionEstablishBase(speedImpactGroup.getMissionEstablishBase());
        target.setMissionExplore(speedImpactGroup.getMissionExplore());
        target.setMissionGather(speedImpactGroup.getMissionGather());
        target.setName(speedImpactGroup.getName());
        if (target.getId() != null) {
            objectRelationToObjectRelationRepository.deleteByMasterId(target.getId());
        }
        target.setImage(speedImpactGroup.getImage());
        target.setRequirementGroups(new ArrayList<>());
        return speedImpactGroupRepository.save(target);
    }

    /**
     * Finds the ids of the unlocked cross galaxy speed impact
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<Integer> findCrossGalaxyUnlocked(UserStorage user) {
        List<SpeedImpactGroup> speedImpactGroups = unlockedRelationBo.unboxToTargetEntity(
                unlockedRelationBo.findByUserIdAndObjectType(user.getId(), ObjectEnum.SPEED_IMPACT_GROUP));
        return speedImpactGroups.stream().map(EntityWithMissionLimitation::getId).collect(Collectors.toList());
    }

    public boolean canIntercept(List<InterceptableSpeedGroup> interceptableSpeedGroups, UserStorage user, Unit unit) {
        SpeedImpactGroup speedImpactGroup = speedImpactGroupFinderBo.findApplicable(user, unit);
        return speedImpactGroup != null && interceptableSpeedGroups.stream().anyMatch(current -> current.getSpeedImpactGroup().getId()
                .equals(speedImpactGroup.getId()));
    }
}
