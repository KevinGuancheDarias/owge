package com.kevinguanchedarias.owgejava.entity.projection;

import com.kevinguanchedarias.owgejava.entity.ImageStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class ObtainedUnitBasicInfoProjection {
    Long id;
    Long count;
    String unitName;
    ImageStore unitImage;
}
