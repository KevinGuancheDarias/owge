package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "planets")
public class Planet implements SimpleIdEntity {
	private static final long serialVersionUID = 1574111685072163032L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private Long sector;
	private Long quadrant;

	@Column(name = "planet_number")
	private Integer planetNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner", nullable = true)
	@Fetch(FetchMode.JOIN)
	private UserStorage owner;

	private Integer richness;
	private Boolean home;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "galaxy_id")
	@Fetch(FetchMode.JOIN)
	private Galaxy galaxy;

	@OneToOne
	@JoinColumn(name = "special_location_id")
	@Cascade({ CascadeType.MERGE, CascadeType.PERSIST })
	private SpecialLocation specialLocation;

	public Double findRationalRichness() {
		return richness / (double) 100;
	}

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getSector() {
		return sector;
	}

	public void setSector(Long sector) {
		this.sector = sector;
	}

	public Long getQuadrant() {
		return quadrant;
	}

	public void setQuadrant(Long quadrant) {
		this.quadrant = quadrant;
	}

	public Integer getPlanetNumber() {
		return planetNumber;
	}

	public void setPlanetNumber(Integer planetNumber) {
		this.planetNumber = planetNumber;
	}

	public UserStorage getOwner() {
		return owner;
	}

	public void setOwner(UserStorage owner) {
		this.owner = owner;
	}

	public Integer getRichness() {
		return richness;
	}

	public void setRichness(Integer richness) {
		this.richness = richness;
	}

	public Boolean getHome() {
		return home;
	}

	public void setHome(Boolean home) {
		this.home = home;
	}

	public Galaxy getGalaxy() {
		return galaxy;
	}

	public void setGalaxy(Galaxy galaxy) {
		this.galaxy = galaxy;
	}

	public SpecialLocation getSpecialLocation() {
		return specialLocation;
	}

	public void setSpecialLocation(SpecialLocation specialLocation) {
		this.specialLocation = specialLocation;
	}

}
