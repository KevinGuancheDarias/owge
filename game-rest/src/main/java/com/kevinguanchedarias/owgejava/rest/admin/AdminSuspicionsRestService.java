package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.audit.AuditMultiAccountSuspicionsService;
import com.kevinguanchedarias.owgejava.dto.SuspicionDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("admin/suspicions")
@ApplicationScope
@AllArgsConstructor
public class AdminSuspicionsRestService {
    private final AuditMultiAccountSuspicionsService auditMultiAccountSuspicionsService;

    @GetMapping
    public List<SuspicionDto> findLast100() {
        return auditMultiAccountSuspicionsService.findLast100();
    }
}
