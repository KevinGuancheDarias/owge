package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;

public class ObtainedUpgradeDto implements DtoFromEntity<ObtainedUpgrade> {
	private Long id;
	private Integer level;
	private Boolean available;
	private UpgradeDto upgrade;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Boolean getAvailable() {
		return available;
	}

	public void setAvailable(Boolean available) {
		this.available = available;
	}

	public UpgradeDto getUpgrade() {
		return upgrade;
	}

	public void setUpgrade(UpgradeDto upgrade) {
		this.upgrade = upgrade;
	}

	@Override
	public void dtoFromEntity(ObtainedUpgrade entity) {
		id = entity.getId();
		level = entity.getLevel();
		available = entity.getAvailable();
		upgrade = new UpgradeDto();
		upgrade.dtoFromEntity(entity.getUpgrade());
	}

}
