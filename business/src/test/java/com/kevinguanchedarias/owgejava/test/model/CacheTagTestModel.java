package com.kevinguanchedarias.owgejava.test.model;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.Builder;
import lombok.Value;

@SuppressWarnings("rawtypes")
@Value
@Builder
public class CacheTagTestModel {
    BaseBo targetBo;
    TaggableCacheManager taggableCacheManager;
    String tag;
}
