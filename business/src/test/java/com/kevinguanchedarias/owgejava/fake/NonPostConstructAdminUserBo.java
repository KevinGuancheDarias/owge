package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.AdminUserBo;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.Serial;

@Service
@Primary
public class NonPostConstructAdminUserBo extends AdminUserBo {
    @Serial
    private static final long serialVersionUID = 4464901458052583266L;

    @Override
    public void init() {
        // Don't invoke postconstruct logic
    }
}
