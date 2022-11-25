package com.kevinguanchedarias.owgejava.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serial;

@Table(name = "obtained_units")
@Entity
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObtainedUnit implements EntityWithCache<Long> {
    public static final String OBTAINED_UNIT_CACHE_TAG = "obtained_unit";

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

    private Long expirationId;

    @Override
    public String getCacheTag() {
        return OBTAINED_UNIT_CACHE_TAG;
    }
}
