package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serial;

@Entity
@Table(name = "mission_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionType implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = -4343475889445744756L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "is_shared", nullable = false)
    private Boolean isShared;
}
