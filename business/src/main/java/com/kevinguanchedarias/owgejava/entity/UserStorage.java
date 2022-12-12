package com.kevinguanchedarias.owgejava.entity;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "user_storage")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserStorage implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = 3718075595543259580L;

    @Id
    @EqualsAndHashCode.Include
    private Integer id;

    private String username;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction", nullable = false)
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Faction faction;

    @Column(name = "last_action")
    private Date lastAction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_planet", nullable = false)
    @ToString.Exclude
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Planet homePlanet;

    @Column(name = "primary_resource")
    private Double primaryResource;

    @Column(name = "secondary_resource")
    private Double secondaryResource;

    @Column(nullable = false)
    private Double energy;

    @Column(name = "has_skipped_tutorial")
    private Boolean hasSkippedTutorial = false;

    @Column(nullable = false)
    private Double points = 0D;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<UnlockedRelation> unlockedRelations;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Mission> missions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alliance_id")
    private Alliance alliance;

    @Column(name = "can_alter_twitch_state", nullable = false)
    private Boolean canAlterTwitchState = false;

    private LocalDateTime lastMultiAccountCheck;
    private Float multiAccountScore;
    private boolean banned = false;

    public void addtoPrimary(Double value) {
        primaryResource += value;
    }

    public void addToSecondary(Double value) {
        secondaryResource += value;
    }
}
