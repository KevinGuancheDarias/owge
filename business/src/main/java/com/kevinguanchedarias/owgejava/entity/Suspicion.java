package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.SuspicionSourceEnum;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suspicions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Suspicion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuspicionSourceEnum source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private UserStorage relatedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    @ToString.Exclude
    private Audit relatedAudit;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
