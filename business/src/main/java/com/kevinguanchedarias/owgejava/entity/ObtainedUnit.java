package com.kevinguanchedarias.owgejava.entity;

import lombok.Data;
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
import java.io.Serial;

@Table(name = "obtained_units")
@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObtainedUnit implements EntityWithId<Long> {
    @Serial
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

    private boolean isFromCapture = false;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }
}
