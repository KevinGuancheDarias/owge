/**
 * 
 */
package com.kevinguanchedarias.sgtjava.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.RankingBo;
import com.kevinguanchedarias.sgtjava.pojo.RankingEntry;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@RequestMapping("ranking")
@ApplicationScope
public class RankingRestService {

	@Autowired
	private RankingBo rankingBo;

	@GetMapping(value = "")
	public List<RankingEntry> findAll() {
		return rankingBo.findRanking();
	}
}
