/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.kevinguanchedarias.owgejava.dto.AbstractImprovementDto;
import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.dto.ImprovementUnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.Improvement;

/**
 * Represents the full sum of a improvement
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class GroupedImprovement extends AbstractImprovementDto {

	/**
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GroupedImprovement() {
		initToZeroes();
	}

	/**
	 * Adds the list of improvements to the group
	 * 
	 * @param improvements
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GroupedImprovement add(List<ImprovementDto> improvements) {
		improvements.forEach(this::doAdd);
		return this;
	}

	/**
	 * Adds one entire group to this group
	 * 
	 * @param groupedImprovement
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GroupedImprovement add(GroupedImprovement groupedImprovement) {
		doAdd(groupedImprovement);
		return this;
	}

	/**
	 * Adds one improvement entity to this group
	 * 
	 * @param improvement
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GroupedImprovement add(Improvement improvement) {
		if (improvement != null) {
			ImprovementDto improvementDto = new ImprovementDto();
			improvementDto.dtoFromEntity(improvement);
			doAdd(improvementDto);
		}
		return this;
	}

	private void doAdd(AbstractImprovementDto improvementDto) {
		if (improvementDto != null) {
			addMoreChargeCapacity(safeSum(improvementDto.getMoreChargeCapacity()));
			addMoreEnergyProduction(safeSum(improvementDto.getMoreEnergyProduction()));
			addMoreMissions(safeSum(improvementDto.getMoreMisions()));
			addMorePrimaryResourceProduction(safeSum(improvementDto.getMorePrimaryResourceProduction()));
			addMoreSecondaryResourceProduction(safeSum(improvementDto.getMoreSecondaryResourceProduction()));
			addMoreUnitBuildSpeed(safeSum(improvementDto.getMoreUnitBuildSpeed()));
			addMoreUpgradeResearchSpeed(safeSum(improvementDto.getMoreUpgradeResearchSpeed()));
			if (improvementDto.getUnitTypesUpgrades() != null) {
				improvementDto.getUnitTypesUpgrades().forEach(this::addToType);
			}
		}
	}

	private Float safeSum(Float numericValue) {
		return ObjectUtils.firstNonNull(numericValue, 0F);
	}

	private void addToType(ImprovementUnitTypeDto improvement) {
		ImprovementUnitTypeDto existing = getUnitTypesUpgrades().stream()
				.filter(current -> current.getType().equals(improvement.getType())
						&& current.getUnitTypeId().equals(improvement.getUnitTypeId()))
				.findFirst().orElse(null);
		if (existing == null) {
			getUnitTypesUpgrades().add(improvement);
		} else {
			existing.setValue(existing.getValue() + improvement.getValue());
		}

	}
}
