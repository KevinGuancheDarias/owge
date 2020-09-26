package com.kevinguanchedarias.owgejava.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.VisitedTutorialSectionEntry;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface VisitedTutorialSectionEntryRepository extends JpaRepository<VisitedTutorialSectionEntry, Long> {
	/**
	 *
	 * @param id
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<VisitedTutorialSectionEntry> findVisitedByUserId(Integer id);
}
