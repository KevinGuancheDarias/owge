package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCacheByUser;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithByUserCacheTagListener;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "missions")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(EntityWithByUserCacheTagListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission implements EntityWithCache<Long>, EntityWithTaggableCacheByUser<Long> {
    public static final String MISSION_CACHE_TAG = "mission";
    public static final String MISSION_BY_USER_CACHE_TAG = "mission_by_user";

    @Serial
    private static final long serialVersionUID = -5258361356566850987L;

    public interface MissionIdAndTerminationDateProjection {
        Long getId();

        Date getDate();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserStorage user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", nullable = false)
    private MissionType type;

    @Column(name = "starting_date")
    private LocalDateTime startingDate = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "termination_date")
    private LocalDateTime terminationDate;

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

    @OneToOne(mappedBy = "mission", cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    private MissionInformation missionInformation;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "related_mission")
    @ToString.Exclude
    private Mission relatedMission;

    @OneToMany(mappedBy = "relatedMission")
    @Getter
    @Setter
    @ToString.Exclude
    private List<Mission> linkedRelated;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private MissionReport report;

    @OneToMany(mappedBy = "mission")
    @ToString.Exclude
    private List<ObtainedUnit> involvedUnits;

    @Column(nullable = false)
    private Integer attemps = 1;

    private Boolean resolved = false;

    @Column(nullable = false)
    private Boolean invisible = false;

    @Override
    public String getCacheTag() {
        return MISSION_CACHE_TAG;
    }

    @Override
    public String getByUserCacheTag() {
        return MISSION_BY_USER_CACHE_TAG;
    }
}