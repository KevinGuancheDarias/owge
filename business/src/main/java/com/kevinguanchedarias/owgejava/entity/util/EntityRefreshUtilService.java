package com.kevinguanchedarias.owgejava.entity.util;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntityRefreshUtilService {
    private final EntityManager entityManager;
    private final ListableBeanFactory beanFactory;

    private Repositories repositories;

    @PostConstruct
    public void init() {
        repositories = new Repositories(beanFactory);
    }

    @SuppressWarnings("unchecked")
    public <K, E extends EntityWithId<K>> E refresh(E object) {
        if (entityManager.contains(object)) {
            entityManager.refresh(object);
            return object;
        } else {
            return (E) repositories.getRepositoryFor(object.getClass())
                    .filter(JpaRepository.class::isInstance)
                    .map(JpaRepository.class::cast)
                    .map(repo -> repo.getReferenceById(object.getId()))
                    .orElseThrow(() -> new IllegalStateException("No repository for " + object.getClass() + ":" + object.getId()));
        }
    }
}
