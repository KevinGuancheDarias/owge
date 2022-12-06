package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 */
@Entity
@Table(name = "system_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMessage implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = 2389321992158592281L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String content;

    @Column(name = "creation_date")
    @Builder.Default
    private LocalDateTime creationDate = LocalDateTime.now(ZoneOffset.UTC);

    @OneToMany(mappedBy = "message", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UserReadSystemMessage> usersRead;
}
