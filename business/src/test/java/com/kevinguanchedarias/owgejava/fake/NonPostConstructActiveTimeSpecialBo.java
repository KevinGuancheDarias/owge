package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.Serial;

@Service
@Primary
public class NonPostConstructActiveTimeSpecialBo extends ActiveTimeSpecialBo {
    @Serial
    private static final long serialVersionUID = 4357713540382558501L;

    @Override
    @PostConstruct
    public void init() {
        // Does nothing
    }

    public void realInit() {
        super.init();
    }
}
