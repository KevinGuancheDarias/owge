package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Data
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

}