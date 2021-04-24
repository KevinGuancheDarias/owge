package com.kevinguanchedarias.owgejava.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.dto.base.DtoWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Getter
@Setter
public class SpeedImpactGroupDto extends DtoWithMissionLimitation implements WithDtoFromEntityTrait<SpeedImpactGroup> {
	private Integer id;
	private String name;
	private Boolean isFixed = false;
	private Double missionExplore = 0D;
	private Double missionGather = 0D;
	private Double missionEstablishBase = 0D;
	private Double missionAttack = 0D;
	private Double missionConquest = 0D;
	private Double missionCounterattack = 0D;
	private Long image;
	private String imageUrl;
	private List<RequirementGroupDto> requirementsGroups;

	@Override
	public void dtoFromEntity(SpeedImpactGroup entity) {
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
		id = entity.getId();
		if (entity.getRequirementGroups() != null) {
			requirementsGroups = entity.getRequirementGroups().stream().map(current -> {
				var dto = new RequirementGroupDto();
				dto.dtoFromEntity(current);
				return dto;
			}).collect(Collectors.toList());
		}
		if (entity.getImage() != null) {
			image = entity.getImage().getId();
			imageUrl = entity.getImage().getUrl();
		}
	}

}
