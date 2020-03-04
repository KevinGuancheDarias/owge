package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "special_locations")
public class SpecialLocation extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
	private static final long serialVersionUID = -4665366711844492367L;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "galaxy_id")
	private Galaxy galaxy;

	@OneToOne(mappedBy = "specialLocation")
	@Fetch(FetchMode.JOIN)
	private Planet assignedPlanet;

	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "improvement_id")
	@Cascade({ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE })
	private Improvement improvement;

	@Column(name = "cloned_improvements")
	private Boolean clonedImprovements = false;

	public Galaxy getGalaxy() {
		return galaxy;
	}

	public void setGalaxy(Galaxy galaxy) {
		this.galaxy = galaxy;
	}

	public Planet getAssignedPlanet() {
		return assignedPlanet;
	}

	public void setAssignedPlanet(Planet assignedPlanet) {
		this.assignedPlanet = assignedPlanet;
	}

	@Override
	public Improvement getImprovement() {
		return improvement;
	}

	@Override
	public void setImprovement(Improvement improvement) {
		this.improvement = improvement;
	}

	@Override
	public Boolean getClonedImprovements() {
		return clonedImprovements;
	}

	@Override
	public void setClonedImprovements(Boolean clonedImprovements) {
		this.clonedImprovements = clonedImprovements;
	}

}
