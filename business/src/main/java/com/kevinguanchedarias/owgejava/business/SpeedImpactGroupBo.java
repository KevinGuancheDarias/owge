package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.dto.SpeedImpactGroupDto;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationToObjectRelationRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class SpeedImpactGroupBo implements BaseBo<Integer, SpeedImpactGroup, SpeedImpactGroupDto> {
	private static final long serialVersionUID = 1367954885113224567L;

	@Autowired
	private SpeedImpactGroupRepository speedImpactGroupRepository;

	@Autowired
	private ObjectRelationToObjectRelationRepository objectRelationToObjectRelationRepository;

	@Autowired
	private RequirementGroupBo requirementGroupBo;

	@Autowired
	private RequirementBo requirementBo;

	@Autowired
	private ObjectRelationBo objectRelationBo;

	@Autowired
	private UnlockedRelationBo unlockedRelationBo;

	@Autowired
	private DtoUtilService dtoUtilService;

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
	 *
	 * @param speedImpactGroupDto
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	@Transactional
	public SpeedImpactGroup save(SpeedImpactGroupDto speedImpactGroupDto) {
		SpeedImpactGroup saved = save(dtoUtilService.entityFromDto(SpeedImpactGroup.class, speedImpactGroupDto));
		speedImpactGroupDto.getRequirementsGroups().forEach(requirementGroupDto -> {
			ObjectRelationToObjectRelation currentGroup = new ObjectRelationToObjectRelation();
			RequirementGroup requirementGroup = requirementGroupBo.save(new RequirementGroup());
			currentGroup.setMaster(
					objectRelationBo.findObjectRelationOrCreate(ObjectEnum.SPEED_IMPACT_GROUP, saved.getId()));
			requirementGroupDto.getRequirements().forEach(requirementInformationDto -> {
				ObjectRelationDto objectRelationDto = new ObjectRelationDto();
				objectRelationDto.dtoFromEntity(requirementGroup.getRelation());
				requirementInformationDto.setRelation(objectRelationDto);
				requirementBo.addRequirementFromDto(requirementInformationDto);
			});
			currentGroup.setSlave(requirementGroupBo.save(requirementGroup).getRelation());
			objectRelationToObjectRelationRepository.save(currentGroup);
		});
		return BaseBo.super.save(saved);

	}

	@Override
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
		return BaseBo.super.save(target);
	}

	/**
	 * Finds the ids of the unlocked cross galaxy speed impact
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<Integer> findCrossGalaxyUnlocked(UserStorage user) {
		List<SpeedImpactGroup> speedImpactGroups = unlockedRelationBo.unboxToTargetEntity(
				unlockedRelationBo.findByUserIdAndObjectType(user.getId(), ObjectEnum.SPEED_IMPACT_GROUP));
		return speedImpactGroups.stream().map(EntityWithMissionLimitation::getId).collect(Collectors.toList());
	}

}
