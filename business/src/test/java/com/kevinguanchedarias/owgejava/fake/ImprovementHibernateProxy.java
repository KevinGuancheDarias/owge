package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.mockito.Mockito;

public class ImprovementHibernateProxy extends Improvement implements HibernateProxy {
    @Override
    public Object writeReplace() {
        return null;
    }

    @Override
    public LazyInitializer getHibernateLazyInitializer() {
        return Mockito.mock(LazyInitializer.class);
    }
}
