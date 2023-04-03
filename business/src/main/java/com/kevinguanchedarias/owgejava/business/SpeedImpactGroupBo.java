package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
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

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
public class SpeedImpactGroupBo implements BaseBo<Integer, SpeedImpactGroup, SpeedImpactGroupDto> {
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
    private DtoUtilService dtoUtilService;

    @Autowired
    @Lazy
    private transient SpeedImpactGroupFinderBo speedImpactGroupFinderBo;

    @Autowired
    private transient RequirementGroupRepository requirementGroupRepository;

    @Autowired
    private transient ObtainedUnitFinderBo obtainedUnitFinderBo;

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

    public boolean canIntercept(List<InterceptableSpeedGroup> interceptableSpeedGroups, UserStorage user, ObtainedUnit obtainedUnit) {
        var unit = obtainedUnitFinderBo.determineTargetUnit(obtainedUnit);
        var speedImpactGroup = speedImpactGroupFinderBo.findApplicable(user, unit);
        return speedImpactGroup != null && interceptableSpeedGroups.stream().anyMatch(current -> current.getSpeedImpactGroup().getId()
                .equals(speedImpactGroup.getId()));
    }
}
