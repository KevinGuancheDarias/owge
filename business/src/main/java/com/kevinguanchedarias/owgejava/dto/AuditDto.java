package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditDto implements DtoFromEntity<Audit> {

    @EqualsAndHashCode.Include
    private Long id;

    private AuditActionEnum action;
    private String actionDetail;
    private String ip;
    private String userAgent;
    private String cookie;

    @Override
    public void dtoFromEntity(Audit entity) {
        id = entity.getId();
        action = entity.getAction();
        actionDetail = entity.getActionDetail();
        ip = entity.getIp();
        userAgent = entity.getUserAgent();
        cookie = entity.getCookie();
    }
}
