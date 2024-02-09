package com.kevinguanchedarias.owgejava.entity.embeddedid;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@Embeddable
public class EventNameUserId implements Serializable {
    @Serial
    private static final long serialVersionUID = -8551694047280262897L;

    @Column(name = "event_name", length = 100)
    private String eventName;

    @Column(name = "user_id")
    private Integer userId;
}
