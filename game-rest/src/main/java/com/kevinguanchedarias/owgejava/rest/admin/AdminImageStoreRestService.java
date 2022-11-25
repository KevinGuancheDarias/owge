/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.ImageStoreDto;
import com.kevinguanchedarias.owgejava.entity.ImageStore;
import com.kevinguanchedarias.owgejava.pojo.UploadImage;
import com.kevinguanchedarias.owgejava.repository.ImageStoreRepository;
import com.kevinguanchedarias.owgejava.rest.trait.WithDeleteRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("admin/image_store")
@AllArgsConstructor
public class AdminImageStoreRestService
        implements WithReadRestServiceTrait<Long, ImageStore, ImageStoreRepository, ImageStoreDto>,
        WithDeleteRestServiceTrait<Long, ImageStore, ImageStoreRepository, ImageStoreDto> {

    private final ImageStoreBo imageStoreBo;
    private final AutowireCapableBeanFactory beanFactory;
    private final ImageStoreRepository imageStoreRepository;

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.CrudWithConfigBuilderTrait#
     * getRestCrudConfigBuilder()
     */
    @Override
    public RestCrudConfigBuilder<Long, ImageStore, ImageStoreRepository, ImageStoreDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Long, ImageStore, ImageStoreRepository, ImageStoreDto> builder = RestCrudConfigBuilder.create();
        return builder.withEntityClass(ImageStore.class).withBeanFactory(beanFactory).withRepository(imageStoreRepository)
                .withDtoClass(ImageStoreDto.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withReadById());
    }

    /**
     * Uploads a new image
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @PostMapping
    public ImageStoreDto uploadImage(@RequestBody UploadImage uploadImage) {
        return imageStoreBo.save(uploadImage.getBase64(), uploadImage.getDisplayName());
    }

    /**
     * Updates modifiable image information
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @PutMapping("{id}")
    public ImageStoreDto updateImage(@RequestBody ImageStoreDto imageStoreDto) {
        return imageStoreBo.update(imageStoreDto);
    }
}
