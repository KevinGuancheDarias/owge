package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCacheByUser;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithByUserCacheTagListener;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
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
@EntityListeners(EntityWithByUserCacheTagListener.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveTimeSpecial implements EntityWithTaggableCacheByUser<Long> {
    public static final String ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG = "active_time_special_by_user";

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

    @Override
    public String getByUserCacheTag() {
        return ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG;
    }
}
