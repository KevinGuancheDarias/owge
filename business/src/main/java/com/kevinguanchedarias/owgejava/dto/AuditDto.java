package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditDto implements WithDtoFromEntityTrait<Audit> {
    private Long id;
    private AuditActionEnum action;
    private String actionDetail;
    private String ip;
    private String userAgent;
    private String cookie;
}
