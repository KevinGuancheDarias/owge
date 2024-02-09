package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "planet_list")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanetList {

    @EmbeddedId
    private PlanetUser planetUser;

    @Column(length = 150)
    private String name;
}
