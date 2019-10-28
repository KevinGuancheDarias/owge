/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.ImageStore;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface ImageStoreRepository extends JpaRepository<ImageStore, Long>, Serializable {
	ImageStore findOneByChecksum(String checksum);
}
