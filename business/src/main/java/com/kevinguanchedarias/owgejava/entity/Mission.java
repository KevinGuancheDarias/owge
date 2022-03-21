package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
import java.io.Serial;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "missions")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission implements EntityWithId<Long> {
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
    private Date startingDate = new Date();

    @Column(name = "termination_date")
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

    @OneToOne(mappedBy = "mission", cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    private MissionInformation missionInformation;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "related_mission")
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
}