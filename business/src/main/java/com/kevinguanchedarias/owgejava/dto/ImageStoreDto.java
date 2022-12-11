/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ImageStore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ImageStoreDto implements DtoFromEntity<ImageStore> {
    @EqualsAndHashCode.Include
    private Long id;

    private String checksum;
    private String filename;
    private String displayName;
    private String description;
    private String url;

    @Override
    public void dtoFromEntity(ImageStore entity) {
        id = entity.getId();
        checksum = entity.getChecksum();
        filename = entity.getFilename();
        displayName = entity.getDisplayName();
        description = entity.getDescription();
        url = entity.getUrl();
    }

    @Override
    public <D extends DtoFromEntity<ImageStore>> List<D> dtoFromEntity(Class<D> targetDtoClass, List<ImageStore> entities) {
        return DtoFromEntity.super.dtoFromEntity(targetDtoClass, entities);
    }
}
