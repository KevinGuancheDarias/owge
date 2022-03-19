package com.kevinguanchedarias.owgejava.test.abstracts;


import com.kevinguanchedarias.owgejava.test.model.CacheTagTestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractBaseBoTest {

    private CacheTagTestModel cacheTagTestModel;

    public abstract CacheTagTestModel findCacheTagInfo();

    @BeforeEach
    public void setup() {
        cacheTagTestModel = findCacheTagInfo();
    }

    @Test
    void getCacheTag_should_work() {
        assertThat(cacheTagTestModel.getTargetBo().getCacheTag()).isEqualTo(cacheTagTestModel.getTag());
    }

    @Test
    void getTaggableCacheManager_should_work() {
        assertThat(cacheTagTestModel.getTargetBo().getTaggableCacheManager()).isEqualTo(cacheTagTestModel.getTaggableCacheManager());
    }
}
