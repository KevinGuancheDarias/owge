package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithTaggableCacheListener;
import com.kevinguanchedarias.owgejava.entity.listener.UnitListener;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;

import java.io.Serial;
import java.util.List;

@ToString(callSuper = true)
@Entity
@Table(name = "units")
@EntityListeners({
        UnitListener.class,
        EntityWithTaggableCacheListener.class
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Unit extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer>, EntityWithTaggableCache<Integer> {
    public static final String UNIT_CACHE_TAG = "unit";

    @Serial
    private static final long serialVersionUID = -1923291486680931835L;

    @Column(name = "display_in_requirements")
    private Boolean hasToDisplayInRequirements;

    @Column(name = "order_number")
    private Integer order;

    private Integer points;
    private Integer time;

    @Column(name = "primary_resource")
    private Integer primaryResource;

    @Column(name = "secondary_resource")
    private Integer secondaryResource;

    private Integer energy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type")
    @Fetch(FetchMode.JOIN)
    private UnitType type;

    private Integer attack;

    @Column(name = "health", nullable = false)
    private Integer health = 1;
    private Integer shield;
    private Integer charge;

    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique = false;

    @Column(name = "can_fast_explore", nullable = false)
    private Boolean canFastExplore = false;

    @Column
    private Double speed = 0D;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "improvement_id")
    @Cascade({CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE})
    private Improvement improvement;

    @Column(name = "cloned_improvements")
    private Boolean clonedImprovements = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speed_impact_group_id")
    @Fetch(FetchMode.JOIN)
    private SpeedImpactGroup speedImpactGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attack_rule_id")
    @Fetch(FetchMode.JOIN)
    private AttackRule attackRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "critical_attack_id")
    @Fetch(FetchMode.JOIN)
    @Getter
    @Setter
    private CriticalAttack criticalAttack;

    @Column(name = "bypass_shield", nullable = false)
    private Boolean bypassShield = false;

    @Column(name = "is_invisible", nullable = false)
    private Boolean isInvisible = false;

    @OneToMany(mappedBy = "unit", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<InterceptableSpeedGroup> interceptableSpeedGroups;

    @Column(nullable = false)
    private Integer storedWeight = 1;

    private Long storageCapacity;

    @Override
    public String getCacheTag() {
        return UNIT_CACHE_TAG;
    }
}
