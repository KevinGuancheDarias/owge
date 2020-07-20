package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.AttackRuleBo;
import com.kevinguanchedarias.owgejava.dto.AttackRuleDto;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@ApplicationScope
@RequestMapping("admin/attack-rule")
public class AdminAttackRuleRestService {
	@Autowired
	private AttackRuleBo attackRuleBo;

	/**
	 *
	 * @param body
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping
	public AttackRuleDto save(@RequestBody AttackRuleDto body) {
		return attackRuleBo.toDto(attackRuleBo.save(body));
	}

	/**
	 *
	 * @param id
	 * @param body
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PutMapping("{id}")
	public AttackRuleDto save(@PathVariable Integer id, @RequestBody AttackRuleDto body) {
		body.setId(id);
		return save(body);
	}

	@DeleteMapping("{id}")
	public void delete(@PathVariable Integer id) {
		attackRuleBo.delete(id);
	}
}
