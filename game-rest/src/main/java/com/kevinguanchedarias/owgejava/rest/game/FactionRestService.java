package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.FactionBo;
import com.kevinguanchedarias.owgejava.dto.FactionDto;

@RestController
@RequestMapping("game/faction")
@ApplicationScope
public class FactionRestService {

	@Autowired
	private FactionBo factionBo;

	@GetMapping("findVisible")
	public List<FactionDto> findVisible() {
		return factionBo.toDto(factionBo.findVisible(false));
	}
}
