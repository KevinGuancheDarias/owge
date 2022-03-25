package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
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
public class Faction extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
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

    @OneToMany(mappedBy = "unitType", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<FactionUnitType> unitTypes;
}
