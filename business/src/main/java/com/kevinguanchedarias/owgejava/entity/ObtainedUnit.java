package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCacheByUser;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithByUserCacheTagListener;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.util.List;

@Table(name = "obtained_units")
@Entity
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(EntityWithByUserCacheTagListener.class)
public class ObtainedUnit implements EntityWithTaggableCacheByUser<Long> {
    public static final String OBTAINED_UNIT_CACHE_TAG_BY_USER = "obtained_unit_by_user";

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

    private boolean isFromCapture = false;

    private Long expirationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_unit_id")
    private ObtainedUnit ownerUnit;

    @OneToMany(mappedBy = "ownerUnit")
    @ToString.Exclude
    private List<ObtainedUnit> storedUnits;


    @Override
    public String getByUserCacheTag() {
        return OBTAINED_UNIT_CACHE_TAG_BY_USER;
    }
}
