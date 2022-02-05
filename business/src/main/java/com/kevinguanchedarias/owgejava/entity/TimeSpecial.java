package com.kevinguanchedarias.owgejava.entity;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Entity
@Table(name = "time_specials")
public class TimeSpecial extends CommonEntityWithImageStore<Integer> implements EntityWithImprovements<Integer> {
    @Serial
    private static final long serialVersionUID = -4022925345261224355L;

    private Long duration;

    @Column(name = "recharge_time")
    private Long rechargeTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "improvement_id")
    @Cascade({CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DELETE})
    private Improvement improvement;

    @Column(name = "cloned_improvements")
    private Boolean clonedImprovements = false;

    /**
     * Duration <b>in seconds</b> of the time special
     *
     * @return the duration
     * @since 0.8.0
     */
    public Long getDuration() {
        return duration;
    }

    /**
     * Duration <b>in seconds</b> of the time special
     *
     * @param duration the duration to set
     * @since 0.8.0
     */
    public void setDuration(Long duration) {
        this.duration = duration;
    }

    /**
     * Time to wait <b>in seconds</b> to be able to use the time special again
     *
     * @return the rechargeTime
     * @since 0.8.0
     */
    public Long getRechargeTime() {
        return rechargeTime;
    }

    /**
     * Time to wait <b>in seconds</b> to be able to use the time special again
     *
     * @param rechargeTime the rechargeTime to set
     * @since 0.8.0
     */
    public void setRechargeTime(Long rechargeTime) {
        this.rechargeTime = rechargeTime;
    }

    @Override
    public Improvement getImprovement() {
        return improvement;
    }

    @Override
    public void setImprovement(Improvement improvement) {
        this.improvement = improvement;
    }

    @Override
    public Boolean getClonedImprovements() {
        return clonedImprovements;
    }

    @Override
    public void setClonedImprovements(Boolean clonedImprovements) {
        this.clonedImprovements = clonedImprovements;
    }

}
