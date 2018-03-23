package com.kevinguanchedarias.sgtjava.entity;

import java.util.Date;

import javax.persistence.CascadeType;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "missions")
public class Mission implements SimpleIdEntity {
	private static final long serialVersionUID = -5258361356566850987L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private UserStorage user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type", nullable = false)
	@Fetch(FetchMode.JOIN)
	private MissionType type;

	@Column(name = "termination_date", nullable = false)
	private Date terminationDate;

	@Column(name = "required_time")
	private Double requiredTime;

	@Column(name = "primary_resource")
	private Double primaryResource;

	@Column(name = "secondary_resource")
	private Double secondaryResource;

	@Column(name = "required_energy")
	private Double requiredEnergy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_planet")
	private Planet sourcePlanet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_planet")
	private Planet targetPlanet;

	@OneToOne(mappedBy = "mission", cascade = CascadeType.ALL, optional = true)
	@Fetch(FetchMode.JOIN)
	private MissionInformation missionInformation;

	@OneToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "related_mission", nullable = true)
	private Mission relatedMission;

	@Column(nullable = true)
	private Boolean resolved = false;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserStorage getUser() {
		return user;
	}

	public void setUser(UserStorage user) {
		this.user = user;
	}

	public MissionType getType() {
		return type;
	}

	public void setType(MissionType type) {
		this.type = type;
	}

	public Date getTerminationDate() {
		return terminationDate;
	}

	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}

	public Double getRequiredTime() {
		return requiredTime;
	}

	public void setRequiredTime(Double requiredTime) {
		this.requiredTime = requiredTime;
	}

	public Double getPrimaryResource() {
		return primaryResource;
	}

	public void setPrimaryResource(Double primaryResource) {
		this.primaryResource = primaryResource;
	}

	public Double getSecondaryResource() {
		return secondaryResource;
	}

	public void setSecondaryResource(Double secondaryResource) {
		this.secondaryResource = secondaryResource;
	}

	public Double getRequiredEnergy() {
		return requiredEnergy;
	}

	public void setRequiredEnergy(Double requiredEnergy) {
		this.requiredEnergy = requiredEnergy;
	}

	public Planet getSourcePlanet() {
		return sourcePlanet;
	}

	public void setSourcePlanet(Planet sourcePlanet) {
		this.sourcePlanet = sourcePlanet;
	}

	public Planet getTargetPlanet() {
		return targetPlanet;
	}

	public void setTargetPlanet(Planet targetPlanet) {
		this.targetPlanet = targetPlanet;
	}

	public MissionInformation getMissionInformation() {
		return missionInformation;
	}

	public void setMissionInformation(MissionInformation missionInformation) {
		this.missionInformation = missionInformation;
	}

	public Mission getRelatedMission() {
		return relatedMission;
	}

	public void setRelatedMission(Mission relatedMission) {
		this.relatedMission = relatedMission;
	}

	/**
	 * If this mission has been solved with success
	 *
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getResolved() {
		return resolved;
	}

	public void setResolved(Boolean resolved) {
		this.resolved = resolved;
	}

}