/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Base64Utils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import com.kevinguanchedarias.kevinsuite.commons.exception.CommonException;
import com.kevinguanchedarias.owgejava.dto.ImageStoreDto;
import com.kevinguanchedarias.owgejava.entity.ImageStore;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.ImageStoreRepository;
import com.kevinguanchedarias.owgejava.trait.WithDisabledSave;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.owgejava.util.ValidationUtil;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class ImageStoreBo implements BaseBo<Long, ImageStore, ImageStoreDto>, WithDisabledSave<ImageStore> {
	private static final long serialVersionUID = -4752052563582076943L;
	private static final Logger LOG = org.apache.log4j.Logger.getLogger(ImageStoreBo.class);

	@Value("${OWGE_DYNAMIC_FILES_PATH:/var/owge_data/dynamic}")
	private String directoryPath;

	@Value("${OWGE_IMAGE_HOST:}")
	private String imageHost;

	@Value("${OWGE_DYNAMIC_URL:dynamic}")
	private String dynamicUrl;

	@Autowired
	@Lazy
	private ImageStoreRepository imageStoreRepository;

	@Autowired
	private DtoUtilService dtoUtilService;

	@Autowired
	private transient ExceptionUtilService exceptionUtilService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
	 */
	@Override
	public JpaRepository<ImageStore, Long> getRepository() {
		return imageStoreRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<ImageStoreDto> getDtoClass() {
		return ImageStoreDto.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.trait.WithDisabledSave#save(java.lang.Object)
	 */
	@Override
	public ImageStore save(ImageStore entity) {
		return WithDisabledSave.super.save(entity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.trait.WithDisabledSave#save(java.util.List)
	 */
	@Override
	public void save(List<ImageStore> entities) {
		WithDisabledSave.super.save(entities);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.trait.WithDisabledSave#saveAndFlush(java.lang
	 * .Object)
	 */
	@Override
	public ImageStore saveAndFlush(ImageStore entity) {
		return WithDisabledSave.super.saveAndFlush(entity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#findAll()
	 */
	@Override
	public List<ImageStore> findAll() {
		List<ImageStore> images = BaseBo.super.findAll();
		images.forEach(this::computeImageUrl);
		return images;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.owgejava.business.BaseBo#findById(java.lang.Number)
	 */
	@Override
	public ImageStore findById(Long id) {
		return computeImageUrl(BaseBo.super.findById(id));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#findByIdOrDie(java.lang.
	 * Number)
	 */
	@Override
	public ImageStore findByIdOrDie(Long id) {
		return computeImageUrl(BaseBo.super.findByIdOrDie(id));
	}

	/**
	 * 
	 * @param checksum
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ImageStore findOneByChecksum(String checksum) {
		return computeImageUrl(this.imageStoreRepository.findOneByChecksum(checksum));
	}

	/**
	 * Saves a new image to the directory and to the database
	 * 
	 * @param base64
	 * @param displayName
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public ImageStoreDto save(String base64, String displayName) {
		String parsedBase64;
		if (base64.indexOf("data:image/") == 0) {
			parsedBase64 = base64.split(",")[1];
		} else {
			parsedBase64 = base64;
		}
		byte[] binaryData = Base64Utils.decodeFromString(parsedBase64);
		String extension = findValidExtension(binaryData);
		String fileName = UUID.nameUUIDFromBytes(binaryData).toString() + extension;
		saveToDisk(binaryData, fileName);
		try {
			String checksum = DigestUtils.md5DigestAsHex(binaryData);
			ImageStore imageStore = new ImageStore();
			imageStore.setDisplayName(displayName + extension);
			imageStore.setFilename(fileName);
			imageStore.setChecksum(checksum);
			ImageStore storedImageStore = findOneByChecksum(checksum);
			storedImageStore = storedImageStore != null ? storedImageStore : BaseBo.super.save(imageStore);
			storedImageStore = computeImageUrl(storedImageStore);
			return dtoUtilService.dtoFromEntity(ImageStoreDto.class, storedImageStore);
		} catch (Exception e) {
			LOG.error("Couldn't save the image to the database", e);
			unlinkFile(fileName);
			throw new CommonException("Not able to save the entity", e);
		}
	}

	/**
	 * Updates the modifiable information of a image
	 * 
	 * @param imageStoreDto
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ImageStoreDto update(ImageStoreDto imageStoreDto) {
		ValidationUtil.getInstance().requireNotNull(imageStoreDto.getId(), "id")
				.requireNonEmptyString(imageStoreDto.getDisplayName(), "displayName")
				.requireNonEmptyString(imageStoreDto.getDescription(), "description");
		ImageStore entity = findByIdOrDie(imageStoreDto.getId());
		entity.setDisplayName(imageStoreDto.getDisplayName());
		entity.setDescription(imageStoreDto.getDescription());
		return dtoUtilService.dtoFromEntity(ImageStoreDto.class, BaseBo.super.save(entity));
	}

	/**
	 * Adds the url to the image store entity from the filename
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ImageStore computeImageUrl(ImageStore imageStore) {
		if (imageStore != null) {
			String schemeAndHost = StringUtils.isEmpty(imageHost) ? "" : imageHost;
			imageStore.setUrl(schemeAndHost + "/" + dynamicUrl + "/" + imageStore.getFilename());
		}
		return imageStore;
	}

	private String findValidExtension(byte[] binaryData) {
		TikaConfig config = TikaConfig.getDefaultConfig();
		Tika tika = new Tika();
		String mediaType = tika.detect(binaryData);
		if (MediaType.IMAGE_PNG_VALUE.equals(mediaType) || MediaType.IMAGE_GIF_VALUE.equals(mediaType)
				|| MediaType.IMAGE_JPEG_VALUE.equals(mediaType)) {
			try {
				MimeType mimeType = config.getMimeRepository().forName(mediaType);
				return mimeType.getExtension();
			} catch (MimeTypeException e) {
				LOG.error("Could not properly handle media type", e);
				throw new CommonException("Media type handling exception", e);
			}
		} else {
			throw exceptionUtilService
					.createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_INVALID_IMAGE_TYPE")
					.withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
		}
	}

	private String saveToDisk(byte[] binaryData, String fileName) {
		Path fileAbsolutePath = new File(directoryPath, fileName).toPath();
		if (!Files.exists(fileAbsolutePath, LinkOption.values())) {
			try (FileOutputStream stream = new FileOutputStream(fileAbsolutePath.toString())) {
				stream.write(binaryData);
			} catch (IOException e) {
				LOG.error("Couldn't save the file to disk", e);
				throw new CommonException("File save failed", e);
			}
		}
		return fileAbsolutePath.toString();
	}

	private void unlinkFile(String fileName) {
		Path fileAbsolute = new File(directoryPath, fileName).toPath();
		if (Files.exists(fileAbsolute, LinkOption.values())) {
			try {
				Files.delete(fileAbsolute);
			} catch (IOException e) {
				String errString = "Couldn't remove file " + fileName;
				LOG.warn(errString, e);
				throw new CommonException(errString, e);
			}
		}
	}

}
