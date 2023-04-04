package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.cache.EntityWithTaggableCache;
import com.kevinguanchedarias.owgejava.entity.listener.EntityWithTaggableCacheListener;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Entity
@Table(name = "time_specials")
@EntityListeners(EntityWithTaggableCacheListener.class)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSpecial extends CommonEntityWithImageStore<Integer>
        implements EntityWithImprovements<Integer>, EntityWithTaggableCache<Integer> {
    public static final String TIME_SPECIAL_CACHE_TAG = "time_special";

    @Serial
    private static final long serialVersionUID = -4022925345261224355L;

    private Long duration;

    @Column(name = "recharge_time")
    private Long rechargeTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "improvement_id")
    @Cascade({CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE})
    private Improvement improvement;

    @Column(name = "cloned_improvements")
    private Boolean clonedImprovements = false;

    @Override
    public String getCacheTag() {
        return TIME_SPECIAL_CACHE_TAG;
    }
}
