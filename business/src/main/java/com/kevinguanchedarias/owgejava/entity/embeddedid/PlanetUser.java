package com.kevinguanchedarias.owgejava.entity.embeddedid;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanetUser implements Serializable {
    @Serial
    private static final long serialVersionUID = -6385505776318319556L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @Fetch(FetchMode.JOIN)
    private UserStorage user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planet_id")
    @Fetch(FetchMode.JOIN)
    private Planet planet;
}
