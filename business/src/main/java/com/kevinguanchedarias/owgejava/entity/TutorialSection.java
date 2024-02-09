package com.kevinguanchedarias.owgejava.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serial;
import java.util.List;

/**
 * Represents a tutorial Section
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "tutorial_sections")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialSection extends CommonEntity<Integer> {
    @Serial
    private static final long serialVersionUID = -8797092312548538813L;

    @Column(name = "frontend_router_path", length = 150)
    private String frontendRouterPath;

    @OneToMany(mappedBy = "tutorialSection")
    @ToString.Exclude
    private transient List<TutorialSectionAvailableHtmlSymbol> availableHtmlSymbols;
}
