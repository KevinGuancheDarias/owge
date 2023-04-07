package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.dto.user.SimpleUserDataDto;
import com.kevinguanchedarias.owgejava.entity.Suspicion;
import com.kevinguanchedarias.owgejava.enumerations.SuspicionSourceEnum;

import java.time.Instant;
import java.time.ZoneOffset;

public record SuspicionDto(
        Long id, SuspicionSourceEnum source, SimpleUserDataDto user, AuditDto audit, Instant createdAt
) {

    public static SuspicionDto of(Suspicion suspicion) {
        var auditDto = new AuditDto();
        auditDto.dtoFromEntity(suspicion.getRelatedAudit());
        return new SuspicionDto(
                suspicion.getId(), suspicion.getSource(), SimpleUserDataDto.of(suspicion.getRelatedUser()), auditDto, suspicion.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
