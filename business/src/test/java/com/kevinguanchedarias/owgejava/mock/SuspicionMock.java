package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.Suspicion;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.SuspicionSourceEnum;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

import static com.kevinguanchedarias.owgejava.mock.AuditMock.givenAudit;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class SuspicionMock {
    public static final long SUSPICION_ID = 1981212234;
    public static final SuspicionSourceEnum SUSPICION_SOURCE = SuspicionSourceEnum.BROWSER_AND_IP;
    public static final UserStorage SUSPICION_USER = givenUser1();
    public static final Audit SUSPICION_AUDIT = givenAudit();
    public static final LocalDateTime SUSPICION_CREATED_AT = LocalDateTime.parse("1993-03-12T20:10:10");
    
    public static Suspicion givenSuspicion() {
        return Suspicion.builder()
                .id(SUSPICION_ID)
                .source(SUSPICION_SOURCE)
                .relatedUser(SUSPICION_USER)
                .relatedAudit(SUSPICION_AUDIT)
                .createdAt(SUSPICION_CREATED_AT)
                .build();
    }
}
