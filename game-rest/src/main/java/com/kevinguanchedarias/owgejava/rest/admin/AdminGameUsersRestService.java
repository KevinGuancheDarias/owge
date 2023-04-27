package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.user.UserDeleteService;
import com.kevinguanchedarias.owgejava.dto.SuspicionDto;
import com.kevinguanchedarias.owgejava.dto.user.SimpleUserDataDto;
import com.kevinguanchedarias.owgejava.dto.user.SimpleUserDataWithSuspicionsCountsDto;
import com.kevinguanchedarias.owgejava.repository.SuspicionRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("admin/users")
@ApplicationScope
@AllArgsConstructor
public class AdminGameUsersRestService {
    private final UserStorageRepository userStorageRepository;
    private final SuspicionRepository suspicionRepository;
    private final UserDeleteService userDeleteService;

    @GetMapping("with-suspicions")
    public List<SimpleUserDataWithSuspicionsCountsDto> findUsers() {
        return userStorageRepository.findAll().stream().map(user ->
                        new SimpleUserDataWithSuspicionsCountsDto(
                                SimpleUserDataDto.of(user),
                                suspicionRepository.countByRelatedUser(user)
                        )
                )
                .sorted(Comparator.comparing(SimpleUserDataWithSuspicionsCountsDto::suspicionsCount).reversed())
                .toList();
    }

    @GetMapping("{id}")
    public SimpleUserDataDto findById(@PathVariable Integer id) {
        return SimpleUserDataDto.of(SpringRepositoryUtil.findByIdOrDie(userStorageRepository, id));
    }

    @DeleteMapping("{id}")
    public void deleteUser(@PathVariable Integer id) {
        userDeleteService.deleteAccount(SpringRepositoryUtil.findByIdOrDie(userStorageRepository, id));
    }

    @GetMapping("{id}/suspicions")
    @Transactional
    public List<SuspicionDto> findUserSuspicions(@PathVariable Integer id) {
        return suspicionRepository.findByRelatedUser(userStorageRepository.getReferenceById(id)).stream()
                .map(SuspicionDto::of)
                .toList();
    }
}
