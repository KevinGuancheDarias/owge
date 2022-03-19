package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.fake.FakeBaseBo;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.fake.FakeBaseBo.CACHE_TAG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BaseBoTest {
    private static final int ENTITY_ID = 8;

    private FakeBaseBo fakeBaseBo;
    private TaggableCacheManager taggableCacheManagerMock;
    private JpaRepository<EntityWithId<Number>, Number> repositoryMock;
    private EntityWithId<Number> entityWithIdMock;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        taggableCacheManagerMock = mock(TaggableCacheManager.class);
        repositoryMock = mock(JpaRepository.class);
        fakeBaseBo = new FakeBaseBo(repositoryMock, taggableCacheManagerMock);

        entityWithIdMock = mock(EntityWithId.class);
    }

    @Test
    void save_should_work_no_evict_id_cache_tag_if_null_id() {
        fakeBaseBo.save(entityWithIdMock);

        verify(repositoryMock, times(1)).save(entityWithIdMock);
        verify(taggableCacheManagerMock, never()).evictByCacheTag(eq(CACHE_TAG), any());
        verifyEvictList();
    }

    @Test
    void save_should_evict_id_cache_tag_if_has_id() {
        given(entityWithIdMock.getId()).willReturn(ENTITY_ID);
        given(repositoryMock.save(entityWithIdMock)).willReturn(entityWithIdMock);
        fakeBaseBo.save(entityWithIdMock);

        verify(repositoryMock, times(1)).save(entityWithIdMock);
        verifyEvictListAndId();
    }

    @Test
    void save_with_list_should_work_no_evict_id_cache_tag_if_null_id() {
        var list = List.of(entityWithIdMock);
        fakeBaseBo.save(list);

        verify(repositoryMock, times(1)).saveAll(list);
        verify(taggableCacheManagerMock, never()).evictByCacheTag(eq(CACHE_TAG), any());
        verifyEvictList();
    }

    @Test
    void save_with_list_should_evict_id_cache_tag_if_has_id() {
        var list = List.of(entityWithIdMock);
        given(entityWithIdMock.getId()).willReturn(ENTITY_ID);
        given(repositoryMock.save(entityWithIdMock)).willReturn(entityWithIdMock);
        fakeBaseBo.save(list);

        verify(repositoryMock, times(1)).saveAll(list);
        verifyEvictListAndId();
    }

    @Test
    void saveAndFlush_should_work_no_evict_id_cache_tag_if_null_id() {
        fakeBaseBo.saveAndFlush(entityWithIdMock);

        verify(repositoryMock, times(1)).saveAndFlush(entityWithIdMock);
        verify(taggableCacheManagerMock, never()).evictByCacheTag(eq(CACHE_TAG), any());
        verifyEvictList();
    }

    @Test
    void saveAndFlush_should_evict_id_cache_tag_if_has_id() {
        given(entityWithIdMock.getId()).willReturn(ENTITY_ID);
        given(repositoryMock.saveAndFlush(entityWithIdMock)).willReturn(entityWithIdMock);
        fakeBaseBo.saveAndFlush(entityWithIdMock);

        verify(repositoryMock, times(1)).saveAndFlush(entityWithIdMock);
        verifyEvictListAndId();
    }

    @Test
    void delete_should_work_with_id() {
        given(this.repositoryMock.findById(ENTITY_ID)).willReturn(Optional.of(entityWithIdMock));
        given(entityWithIdMock.getId()).willReturn(ENTITY_ID);

        fakeBaseBo.delete(ENTITY_ID);

        verify(repositoryMock, times(1)).delete(entityWithIdMock);
        verifyEvictListAndId();
    }

    @Test
    void delete_should_work_with_entity() {
        given(entityWithIdMock.getId()).willReturn(ENTITY_ID);

        fakeBaseBo.delete(entityWithIdMock);

        verify(repositoryMock, times(1)).delete(entityWithIdMock);
        verifyEvictListAndId();
    }

    @Test
    void delete_should_work_with_list() {
        given(entityWithIdMock.getId()).willReturn(ENTITY_ID);

        fakeBaseBo.delete(List.of(entityWithIdMock));

        verify(repositoryMock, times(1)).deleteAll(List.of(entityWithIdMock));
        verifyEvictListAndId();
    }

    private void verifyEvictList() {
        verify(taggableCacheManagerMock, times(1)).evictByCacheTag(CACHE_TAG);
    }

    private void verifyEvictListAndId() {
        verifyEvictList();
        verify(taggableCacheManagerMock, times(1)).evictByCacheTag(CACHE_TAG, ENTITY_ID);
    }

}
