package com.kevinguanchedarias.owgejava.entity;

import lombok.*;

import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Table(name = "planets")
@Data
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Planet implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = 1574111685072163032L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    private Long sector;
    private Long quadrant;

    @Column(name = "planet_number")
    private Integer planetNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    private UserStorage owner;

    private Integer richness;
    private Boolean home;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "galaxy_id")
    private Galaxy galaxy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "special_location_id")
    private SpecialLocation specialLocation;

    public Double findRationalRichness() {
        return richness / (double) 100;
    }
}
