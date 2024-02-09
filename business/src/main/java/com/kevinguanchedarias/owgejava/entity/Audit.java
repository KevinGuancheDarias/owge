package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import lombok.*;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Audit implements EntityWithId<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditActionEnum action;

    private String actionDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserStorage user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_id")
    private UserStorage relatedUser;

    private String ipv4;
    private String ipv6;

    private String userAgent;
    private String cookie;
    private boolean isTor;

    @Column(nullable = false)
    private LocalDateTime creationDate;

    public String findIp() {
        return ipv4 == null ? ipv6 : ipv4;
    }
}
