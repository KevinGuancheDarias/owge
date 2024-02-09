package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import lombok.*;

import jakarta.persistence.*;

import java.io.Serial;
import java.util.List;

/**
 * Represents a Faction
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "factions")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(EntityWithTaggableCache.class)
public class Faction extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer>, EntityWithTaggableCache<Integer> {
    public static final String FACTION_CACHE_TAG = "faction";

    @Serial
    private static final long serialVersionUID = -8190094507195501651L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Boolean hidden;

    @Column(name = "primary_resource_name")
    private String primaryResourceName;

    @Column(name = "secondary_resource_name")
    private String secondaryResourceName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_resource_image_id")
    private ImageStore primaryResourceImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_resource_image_id")
    private ImageStore secondaryResourceImage;

    @Column(name = "energy_name")
    private String energyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "energy_image_id")
    private ImageStore energyImage;

    @Column(name = "initial_primary_resource")
    private Integer initialPrimaryResource;

    @Column(name = "initial_secondary_resource")
    private Integer initialSecondaryResource;

    @Column(name = "initial_energy")
    private Integer initialEnergy;

    @Column(name = "primary_resource_production")
    private Float primaryResourceProduction;

    @Column(name = "secondary_resource_production")
    private Float secondaryResourceProduction;

    @Column(name = "max_planets", nullable = false)
    private Integer maxPlanets;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "improvement_id")
    @ToString.Exclude
    private Improvement improvement;

    @Column(name = "cloned_improvements")
    private Boolean clonedImprovements = false;

    @Column(name = "custom_primary_gather_percentage")
    private Float customPrimaryGatherPercentage = 0F;

    @Column(name = "custom_secondary_gather_percentage")
    private Float customSecondaryGatherPercentage = 0F;

    @OneToMany(mappedBy = "faction", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<UserStorage> users;

    @OneToMany(mappedBy = "faction", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<FactionUnitType> unitTypes;

    @Override
    public String getCacheTag() {
        return FACTION_CACHE_TAG;
    }
}
