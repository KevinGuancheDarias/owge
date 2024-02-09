package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serial;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Table(name = "translatables_translations")
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslatableTranslation implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = 8499028692222579343L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translatable_id")
    private Translatable translatable;

    @Column(name = "lang_code", length = 2, nullable = false)
    private String langCode;

    private String value;
}
