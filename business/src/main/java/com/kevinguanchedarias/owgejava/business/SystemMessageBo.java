package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.SystemMessageDto;
import com.kevinguanchedarias.owgejava.entity.SystemMessage;
import com.kevinguanchedarias.owgejava.entity.UserReadSystemMessage;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.SystemMessageUser;
import com.kevinguanchedarias.owgejava.repository.SystemMessageRepository;
import com.kevinguanchedarias.owgejava.repository.UserReadSystemMessageRepository;
import com.kevinguanchedarias.owgejava.util.EntityUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 */
@Service
@AllArgsConstructor
public class SystemMessageBo implements BaseBo<Integer, SystemMessage, SystemMessageDto> {
    public static final String SYSTEM_MESSAGE_CACHE_TAG = "system_message";

    @Serial
    private static final long serialVersionUID = 2748430747376904932L;

    private final transient SystemMessageRepository repository;
    private final transient TransactionUtilService transactionUtilService;
    private final transient SocketIoService socketIoService;
    private final UserStorageBo userStorageBo;
    private final transient UserReadSystemMessageRepository userReadRepository;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void deleteOldSystemMessages() {
        transactionUtilService.runWithRequired(() -> {
            List<SystemMessage> forDeleteMessages = repository
                    .findByCreationDateLessThan(new Date(new Date().getTime() - (86400 * 1000 * 7)));
            if (!forDeleteMessages.isEmpty()) {
                repository.deleteAll(forDeleteMessages);
            }
        });
    }

    /**
     * Note, it sends a websocket event
     *
     * @since 0.9.16
     */
    @Override
    @Transactional
    public SystemMessage save(SystemMessageDto systemMessageDto) {
        SystemMessage systemMessage = new SystemMessage();
        BeanUtils.copyProperties(systemMessageDto, systemMessage);
        EntityUtil.requireNullId(systemMessage);
        systemMessage = repository.save(systemMessage);
        transactionUtilService.doAfterCommit(this::emitChange);
        return systemMessage;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     */
    public List<SystemMessageUser> findReadByUser(int userId) {
        return findAll(Sort.by(Direction.DESC, "id")).stream().map(message -> translateToUser(message, userId))
                .collect(Collectors.toList());
    }

    /**
     * Translates the specified system message to one object with the isRead
     * property defined
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     */
    public SystemMessageUser translateToUser(SystemMessage message, int userId) {
        SystemMessageUser systemMessageUser = new SystemMessageUser();
        systemMessageUser.setId(message.getId());
        systemMessageUser.setContent(message.getContent());
        systemMessageUser.setCreationDate(message.getCreationDate());
        systemMessageUser.setIsRead(userReadRepository.existsByMessageIdAndUserId(message.getId(), userId));
        return systemMessageUser;
    }

    @Transactional
    public void markAsRead(List<Integer> messages, UserStorage user) {
        repository.findAllById(messages).forEach(message -> {
            UserReadSystemMessage userRead = new UserReadSystemMessage();
            userRead.setMessage(message);
            userRead.setUser(user);
            userReadRepository.save(userRead);
        });
        transactionUtilService.doAfterCommit(() -> emitChangeToUser(user.getId()));
    }

    @Override
    public Class<SystemMessageDto> getDtoClass() {
        return SystemMessageDto.class;
    }

    @Override
    public JpaRepository<SystemMessage, Integer> getRepository() {
        return repository;
    }

    private void emitChange() {
        List<SystemMessage> messages = findAll();
        userStorageBo.findAllIds().forEach(userId -> emitChangeToUser(messages, userId));
    }

    private void emitChangeToUser(int userId) {
        emitChangeToUser(findAll(), userId);
    }

    private void emitChangeToUser(List<SystemMessage> messages, int userId) {
        socketIoService.sendMessage(userId, "system_message_change",
                () -> messages.stream().map(message -> translateToUser(message, userId)).collect(Collectors.toList()));
    }
}
