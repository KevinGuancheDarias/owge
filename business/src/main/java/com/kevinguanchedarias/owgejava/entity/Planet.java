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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serial;

@Entity
@Table(name = "planets")
@Data
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
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
