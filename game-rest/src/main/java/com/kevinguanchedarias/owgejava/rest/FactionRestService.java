package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.FactionBo;

@RestController
@ApplicationScope
public class FactionRestService {

	@Autowired
	private FactionBo factionBo;

	@RequestMapping(value = "/faction/findVisible", method = RequestMethod.GET)
	public Object findVisible() {
		return factionBo.findVisible(false);
	}

	@RequestMapping(value = "/game/faction/findVisible", method = RequestMethod.GET)
	public Object findVisible2() {
		return factionBo.findVisible(false);
	}
}
