package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "improvements")
public class Improvement extends ImprovementBase {
	private static final long serialVersionUID = 2210759653999657847L;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "improvementId")
	@Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE })
	@Fetch(FetchMode.JOIN)
	private List<ImprovementUnitType> unitTypesUpgrades;

	public List<ImprovementUnitType> getUnitTypesUpgrades() {
		return unitTypesUpgrades;
	}

	public void setUnitTypesUpgrades(List<ImprovementUnitType> unitTypesUpgrades) {
		this.unitTypesUpgrades = unitTypesUpgrades;
	}

}
