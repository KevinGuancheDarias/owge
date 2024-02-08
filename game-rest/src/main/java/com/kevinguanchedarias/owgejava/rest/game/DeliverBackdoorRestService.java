package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

@RestController
@RequestMapping("game/deliver-backdoor")
@ApplicationScope
@AllArgsConstructor
public class DeliverBackdoorRestService {

    private final SocketIoService socketIoService;
    private final PlanetLockUtilService planetLockUtilService;

    @GetMapping("ping-user")
    @Transactional
    public boolean pingUser(@RequestParam("targetUser") Integer targetUserId) {
        var targetUser = new UserStorage();
        targetUser.setId(targetUserId);
        socketIoService.sendMessage(targetUser, "ping", () -> "Hello World");
        return true;
    }
}
