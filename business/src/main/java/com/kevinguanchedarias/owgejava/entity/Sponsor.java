package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.SponsorTypeEnum;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * The type Sponsor.
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.21
 */
@Entity
@Table(name = "sponsors")
@Getter
@Setter
public class Sponsor extends CommonEntityWithImageStore<Integer> {
    @Serial
    private static final long serialVersionUID = -8178529676124231495L;

    private String url;

    @Enumerated(EnumType.STRING)
    private SponsorTypeEnum type;
}
