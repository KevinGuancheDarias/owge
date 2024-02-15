package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.entity.util.EntityRefreshUtilService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class NonPostConstructEntityRefreshUtilService extends EntityRefreshUtilService {
    public NonPostConstructEntityRefreshUtilService(EntityManager entityManager, ListableBeanFactory beanFactory) {
        super(entityManager, beanFactory);
    }

    @Override
    public void init() {
        // Do nothing on init
    }
    
    public void realInit() {
        super.init();
    }
}
