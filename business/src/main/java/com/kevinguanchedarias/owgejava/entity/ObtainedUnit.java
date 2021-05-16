package com.kevinguanchedarias.owgejava.entity;

import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "obtained_units")
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObtainedUnit implements EntityWithId<Long> {
    private static final long serialVersionUID = -373057104230776167L;

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserStorage user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    private Long count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_planet")
    private Planet sourcePlanet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_planet")
    private Planet targetPlanet;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_deployment_mission")
    private Mission firstDeploymentMission;

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

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
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

    public Mission getMission() {
        return mission;
    }

    public void setMission(Mission mission) {
        this.mission = mission;
    }

    /**
     * @return the firstDeploymentMission
     * @since 0.7.4
     */
    public Mission getFirstDeploymentMission() {
        return firstDeploymentMission;
    }

    /**
     * @param firstDeploymentMission the firstDeploymentMission to set
     * @since 0.7.4
     */
    public void setFirstDeploymentMission(Mission firstDeploymentMission) {
        this.firstDeploymentMission = firstDeploymentMission;
    }

}
