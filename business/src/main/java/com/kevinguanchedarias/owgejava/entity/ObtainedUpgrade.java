package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serial;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "obtained_upgrades")
public class ObtainedUpgrade implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = 2859853666452009827L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Fetch(FetchMode.JOIN)
    private UserStorage userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upgrade_id", nullable = false)
    @Fetch(FetchMode.JOIN)
    private Upgrade upgrade;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Boolean available;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated use {@link ObtainedUpgrade#getUser()}
     */
    @Deprecated(since = "0.9.0")
    public UserStorage getUserId() {
        return userId;
    }

    /**
     * @param userId
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @deprecated use {@link ObtainedUpgrade#setUser(UserStorage)}
     */
    @Deprecated
    public void setUserId(UserStorage userId) {
        this.userId = userId;
    }

    /**
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public UserStorage getUser() {
        return userId;
    }

    /**
     * @param user
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void setUser(UserStorage user) {
        userId = user;
    }

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(Upgrade upgrade) {
        this.upgrade = upgrade;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
     * @return
     * @deprecated Use isAvailable instead()
     */
    @Deprecated(since = "0.8.1")
    public Boolean getAvailable() {
        return available;
    }

    /**
     * @return
     * @since 0.8.1
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

}