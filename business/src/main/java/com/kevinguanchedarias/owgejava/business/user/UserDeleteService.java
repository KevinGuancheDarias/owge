package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDeleteService {
    public static final String ACCOUNT_DELETED = "account_deleted";
    private final UserStorageRepository userStorageRepository;
    private final SocketIoService socketIoService;

    @Autowired
    @Lazy
    private List<UserDeleteListener> userDeleteListeners;

    @Transactional
    public void deleteAccount(UserStorage user) {
        this.userDeleteListeners.stream()
                .sorted(Comparator.comparing(UserDeleteListener::order))
                .forEach(listener -> listener.doDeleteUser(user));
        this.userStorageRepository.delete(user);
        socketIoService.sendOneTimeMessage(user.getId(), ACCOUNT_DELETED, () -> true, null);
    }
}
