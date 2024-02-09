package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Represents a join request for an alliance
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alliance_join_request")
public class AllianceJoinRequest implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -5567072839053714253L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "alliance_id", nullable = false)
    @Fetch(FetchMode.JOIN)
    private Alliance alliance;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @Fetch(FetchMode.JOIN)
    private UserStorage user;

    @Column(name = "request_date", nullable = false)
    @Builder.Default
    private LocalDateTime requestDate = LocalDateTime.now(ZoneOffset.UTC);
}
