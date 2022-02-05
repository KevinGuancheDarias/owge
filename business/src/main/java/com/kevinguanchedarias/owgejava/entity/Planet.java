package com.kevinguanchedarias.owgejava.entity;

import lombok.EqualsAndHashCode;
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
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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
    @JoinColumn(name = "owner", nullable = true)
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

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSector() {
        return sector;
    }

    public void setSector(Long sector) {
        this.sector = sector;
    }

    public Long getQuadrant() {
        return quadrant;
    }

    public void setQuadrant(Long quadrant) {
        this.quadrant = quadrant;
    }

    public Integer getPlanetNumber() {
        return planetNumber;
    }

    public void setPlanetNumber(Integer planetNumber) {
        this.planetNumber = planetNumber;
    }

    public UserStorage getOwner() {
        return owner;
    }

    public void setOwner(UserStorage owner) {
        this.owner = owner;
    }

    public Integer getRichness() {
        return richness;
    }

    public void setRichness(Integer richness) {
        this.richness = richness;
    }

    public Boolean getHome() {
        return home;
    }

    public void setHome(Boolean home) {
        this.home = home;
    }

    public Galaxy getGalaxy() {
        return galaxy;
    }

    public void setGalaxy(Galaxy galaxy) {
        this.galaxy = galaxy;
    }

    public SpecialLocation getSpecialLocation() {
        return specialLocation;
    }

    public void setSpecialLocation(SpecialLocation specialLocation) {
        this.specialLocation = specialLocation;
    }

}
