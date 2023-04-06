package com.kevinguanchedarias.owgejava.entity.jdbc;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ObtainedUnitTemporalInformation {
    @Id
    private Long id;

    private Long duration;
    private Instant expiration;
    private Integer relationId;

    @Transient
    private Long pendingMillis;
}
