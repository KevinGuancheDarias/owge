package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
 * Represents a referenced html element that is available in a
 * {@link TutorialSection}
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "tutorial_sections_available_html_symbols")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialSectionAvailableHtmlSymbol implements EntityWithId<Integer> {
    @Serial
    private static final long serialVersionUID = 5287466924892458677L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50)
    private String name;

    @Column(length = 150)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutorial_section_id")
    @Fetch(FetchMode.JOIN)
    private TutorialSection tutorialSection;
}
