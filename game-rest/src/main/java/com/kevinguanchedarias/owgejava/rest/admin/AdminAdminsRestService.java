package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.AdminUserBo;
import com.kevinguanchedarias.owgejava.dto.AdminUserDto;

@RestController
@RequestMapping("admin/admin-user")
@ApplicationScope
public class AdminAdminsRestService {
	@Autowired
	private AdminUserBo adminUserBo;

	@GetMapping
	public List<AdminUserDto> findAll() {
		return adminUserBo.toDto(adminUserBo.findAll());
	}

	@PutMapping("{id}")
	public AdminUserDto add(@PathVariable Integer id, @RequestBody AdminUserDto adminUserDto) {
		return adminUserBo.toDto(adminUserBo.addAdmin(id, adminUserDto.getUsername()));
	}

	@DeleteMapping("{id}")
	public void delete(@PathVariable Integer id) {
		adminUserBo.delete(id);
	}
}
