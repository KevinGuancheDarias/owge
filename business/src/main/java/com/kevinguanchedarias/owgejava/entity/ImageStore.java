package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.ImageStoreListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serial;

/**
 * Represents an image stored in the server
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Entity
@Table(name = "images_store")
@Data
@EntityListeners(ImageStoreListener.class)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ImageStore implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = 2646635871850664581L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(length = 32, nullable = false)
    private String checksum;

    @Column(length = 500, nullable = false)
    private String filename;

    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(length = 200)
    private String description = "";

    @Transient
    private String url;
}
