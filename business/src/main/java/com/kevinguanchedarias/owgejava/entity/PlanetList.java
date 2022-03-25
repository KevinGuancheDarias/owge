package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

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
