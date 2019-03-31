package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.UniverseBo;

@RestController
@RequestMapping("universe")
@ApplicationScope
public class UniverseRestService {

	@Autowired
	private UniverseBo universeBo;

	@RequestMapping(value = "findOfficials", method = RequestMethod.GET)
	public Object findOfficials() {
		return universeBo.findOfficialUniverses();
	}
}