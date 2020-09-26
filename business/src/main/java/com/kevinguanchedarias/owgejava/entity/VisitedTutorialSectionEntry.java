package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Table(name = "visited_tutorial_entries")
@Entity
public class VisitedTutorialSectionEntry {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private UserStorage user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entry_id")
	private TutorialSectionEntry entry;

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the user
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserStorage getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUser(UserStorage user) {
		this.user = user;
	}

	/**
	 * @return the entry
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TutorialSectionEntry getEntry() {
		return entry;
	}

	/**
	 * @param entry the entry to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEntry(TutorialSectionEntry entry) {
		this.entry = entry;
	}

}
