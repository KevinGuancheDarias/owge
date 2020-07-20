package com.kevinguanchedarias.owgejava.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class AttackRuleDto implements WithDtoFromEntityTrait<AttackRule> {
	private Integer id;
	private String name;
	private List<AttackRuleEntryDto> entries;

	@Override
	public void dtoFromEntity(AttackRule entity) {
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
		if (entity.getAttackRuleEntries() != null) {
			entries = entity.getAttackRuleEntries().stream().map(entry -> {
				AttackRuleEntryDto dto = new AttackRuleEntryDto();
				dto.dtoFromEntity(entry);
				return dto;
			}).collect(Collectors.toList());
		} else {
			entries = new ArrayList<>();
		}
	}

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the name
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the entries
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<AttackRuleEntryDto> getEntries() {
		return entries;
	}

	/**
	 * @param entries the entries to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEntries(List<AttackRuleEntryDto> entries) {
		this.entries = entries;
	}

}
