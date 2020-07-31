package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.kevinguanchedarias.owgejava.entity.listener.AttackRuleEntityListener;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "attack_rules")
@EntityListeners(AttackRuleEntityListener.class)
public class AttackRule implements EntityWithId<Integer> {
	private static final long serialVersionUID = -8949527349751479310L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 100)
	private String name;

	@OneToMany(mappedBy = "attackRule")
	private transient List<AttackRuleEntry> attackRuleEntries;

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	@Override
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
	 * @return the attackRuleEntries
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<AttackRuleEntry> getAttackRuleEntries() {
		return attackRuleEntries;
	}

	/**
	 * @param attackRuleEntries the attackRuleEntries to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAttackRuleEntries(List<AttackRuleEntry> attackRuleEntries) {
		this.attackRuleEntries = attackRuleEntries;
	}

}
