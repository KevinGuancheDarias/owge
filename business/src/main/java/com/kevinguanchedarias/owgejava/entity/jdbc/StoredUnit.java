package com.kevinguanchedarias.owgejava.entity.jdbc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoredUnit {
    @Id
    private Long id;

    private Long ownerObtainedUnitId;
    private Long targetObtainedUnitId;
}
