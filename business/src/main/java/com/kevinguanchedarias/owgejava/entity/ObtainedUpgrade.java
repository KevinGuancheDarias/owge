package com.kevinguanchedarias.owgejava.entity;

import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;

import java.io.Serial;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "obtained_upgrades")
public class ObtainedUpgrade implements EntityWithCache<Long> {
    public static final String OBTAINED_UPGRADE_CACHE_TAG = "obtained_upgrade";

    @Serial
    private static final long serialVersionUID = 2859853666452009827L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Fetch(FetchMode.JOIN)
    private UserStorage user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upgrade_id", nullable = false)
    @Fetch(FetchMode.JOIN)
    private Upgrade upgrade;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    @Setter
    private Boolean available;

    /**
     * @since 0.8.1
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }

    @Override
    public String getCacheTag() {
        return OBTAINED_UPGRADE_CACHE_TAG;
    }
}