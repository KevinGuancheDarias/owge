package com.kevinguanchedarias.owgejava.entity;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "missions")
public class Mission implements EntityWithId<Long> {
	private static final long serialVersionUID = -5258361356566850987L;

	public interface MissionIdAndTerminationDateProjection {
		Long getId();

		Date getDate();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private UserStorage user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type", nullable = false)
	private MissionType type;

	@Column(name = "starting_date")
	private Date startingDate = new Date();

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

	@OneToOne(fetch = FetchType.LAZY, optional = true, cascade = CascadeType.REMOVE)
	@JoinColumn(name = "related_mission", nullable = true)
	private Mission relatedMission;

	@OneToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "report_id", nullable = true)
	private MissionReport report;

	@OneToMany(mappedBy = "mission")
	private List<ObtainedUnit> involvedUnits;

	@Column(nullable = false)
	private Integer attemps = 1;

	@Column(nullable = true)
	private Boolean resolved = false;

	@Override
	public Long getId() {
		return id;
	}

	@Override
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

	/**
	 * @return the startingDate
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Date getStartingDate() {
		return startingDate;
	}

	/**
	 * @param startingDate the startingDate to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.6
	 */
	public void setStartingDate(Date startingDate) {
		this.startingDate = startingDate;
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

	public MissionReport getReport() {
		return report;
	}

	public void setReport(MissionReport report) {
		this.report = report;
	}

	public List<ObtainedUnit> getInvolvedUnits() {
		return involvedUnits;
	}

	public void setInvolvedUnits(List<ObtainedUnit> involvedUnits) {
		this.involvedUnits = involvedUnits;
	}

	public Integer getAttemps() {
		return attemps;
	}

	public void setAttemps(Integer attemps) {
		this.attemps = attemps;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Mission other = (Mission) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

}