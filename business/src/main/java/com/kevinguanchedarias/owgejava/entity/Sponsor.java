package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.SponsorTypeEnum;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
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
