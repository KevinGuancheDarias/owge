package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.listener.TranslatableListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@EntityListeners(TranslatableListener.class)
@Table(name = "translatables")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Translatable implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = -5334666049127689262L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "default_lang_code", length = 2, nullable = false)
    private String defaultLangCode = "en";

    @Transient
    @ToString.Exclude
    private TranslatableTranslation translation;

}
