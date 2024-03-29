package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.dto.user.SimpleUserDataDto;
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
    private String ipv6;
    private String userAgent;
    private String cookie;
    private SimpleUserDataDto user;

    @Override
    public void dtoFromEntity(Audit entity) {
        id = entity.getId();
        action = entity.getAction();
        actionDetail = entity.getActionDetail();
        ip = entity.getIpv4();
        ipv6 = entity.getIpv6();
        userAgent = entity.getUserAgent();
        cookie = entity.getCookie();
        user = SimpleUserDataDto.of(entity.getUser());
    }
}
