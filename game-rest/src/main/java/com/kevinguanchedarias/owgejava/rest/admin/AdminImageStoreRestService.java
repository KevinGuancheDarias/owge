/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.ImageStoreDto;
import com.kevinguanchedarias.owgejava.entity.ImageStore;
import com.kevinguanchedarias.owgejava.pojo.UploadImage;
import com.kevinguanchedarias.owgejava.rest.trait.WithDeleteRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@Scope()
@RequestMapping("admin/image_store")
public class AdminImageStoreRestService
		implements WithReadRestServiceTrait<Long, ImageStore, ImageStoreBo, ImageStoreDto>,
		WithDeleteRestServiceTrait<Long, ImageStore, ImageStoreBo, ImageStoreDto> {

	@Autowired
	private ImageStoreBo imageStoreBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.CrudWithConfigBuilderTrait#
	 * getRestCrudConfigBuilder()
	 */
	@Override
	public RestCrudConfigBuilder<Long, ImageStore, ImageStoreBo, ImageStoreDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Long, ImageStore, ImageStoreBo, ImageStoreDto> builder = RestCrudConfigBuilder.create();
		return builder.withEntityClass(ImageStore.class).withBeanFactory(beanFactory).withBoService(imageStoreBo)
				.withDtoClass(ImageStoreDto.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withReadById());
	}

	/**
	 * Uploads a new image
	 * 
	 * @param uploadImage
	 * @return
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
	 * @param imageStoreDto
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PutMapping("{id}")
	public ImageStoreDto updateImage(@RequestBody ImageStoreDto imageStoreDto) {
		return imageStoreBo.update(imageStoreDto);
	}
}
