package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.ImageStore;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ImageStoreMock {
    public static final long IMAGE_ID = 582;
    public static final String IMAGE_URL = "/foo";

    public static ImageStore givenImageStore() {
        return ImageStore.builder()
                .id(IMAGE_ID)
                .url(IMAGE_URL)
                .build();

    }
}
