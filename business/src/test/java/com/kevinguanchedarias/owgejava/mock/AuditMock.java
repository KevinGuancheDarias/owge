package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;

@UtilityClass
public class AuditMock {
    public static long AUDIT_ID = 82783;
    public static String AUDIT_IP = "192.168.74.254";
    public static String AUDIT_IPV6 = "lo80";
    public static String AUDIT_USER_AGENT = "Chrome foo";
    public static String AUDIT_COOKIE = "FooCookie";
    public static AuditActionEnum AUDIT_ACTION = AuditActionEnum.ACCEPT_JOIN_ALLIANCE;

    public static Audit givenAudit() {
        return Audit.builder()
                .id(AUDIT_ID)
                .ipv4(AUDIT_IP)
                .ipv6(AUDIT_IPV6)
                .action(AUDIT_ACTION)
                .userAgent(AUDIT_USER_AGENT)
                .cookie(AUDIT_COOKIE)
                .user(givenUser1())
                .build();
    }
}
