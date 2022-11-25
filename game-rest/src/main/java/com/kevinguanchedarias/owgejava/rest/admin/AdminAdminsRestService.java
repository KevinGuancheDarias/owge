package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.AdminUserBo;
import com.kevinguanchedarias.owgejava.dto.AdminUserDto;
import com.kevinguanchedarias.owgejava.repository.AdminUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;

@RestController
@RequestMapping("admin/admin-user")
@ApplicationScope
@AllArgsConstructor
public class AdminAdminsRestService {
    private final AdminUserBo adminUserBo;
    private final AdminUserRepository adminUserRepository;

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
        adminUserRepository.deleteById(id);
    }
}
