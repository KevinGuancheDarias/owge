package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.RequirementBo;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serial;

@Service
@Primary
public class NonPostConstructRequirementBo extends RequirementBo {

    @Serial
    private static final long serialVersionUID = -3154640390017555899L;

    @Override
    @PostConstruct
    public void init() {
        // Do nothing
    }

    public void realInit() {
        super.init();
    }
}
