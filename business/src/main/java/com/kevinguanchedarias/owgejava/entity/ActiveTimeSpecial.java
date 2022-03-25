package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serial;
import java.util.Date;

/**
 * Represents an active TimeSpecial
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Entity
@Table(name = "active_time_specials")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveTimeSpecial implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = 3520607499758897866L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserStorage user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_special_id")
    @Fetch(FetchMode.JOIN)
    private TimeSpecial timeSpecial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSpecialStateEnum state;

    @Column(name = "activation_date", nullable = false)
    private Date activationDate;

    @Column(name = "expiring_date", nullable = false)
    private Date expiringDate;

    @Column(name = "ready_date")
    private Date readyDate;

    @Transient
    private Long pendingTime;
}
