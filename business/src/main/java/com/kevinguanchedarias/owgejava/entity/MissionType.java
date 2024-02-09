package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
