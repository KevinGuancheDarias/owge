package com.kevinguanchedarias.owgejava.business;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.SystemMessageDto;
import com.kevinguanchedarias.owgejava.entity.SystemMessage;
import com.kevinguanchedarias.owgejava.entity.UserReadSystemMessage;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.SystemMessageUser;
import com.kevinguanchedarias.owgejava.repository.SystemMessageRepository;
import com.kevinguanchedarias.owgejava.repository.UserReadSystemMessageRepository;
import com.kevinguanchedarias.owgejava.util.EntityUtil;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class SystemMessageBo implements BaseBo<Integer, SystemMessage, SystemMessageDto> {
	private static final long serialVersionUID = 2748430747376904932L;

	@Autowired
	private transient SystemMessageRepository repository;

	@Autowired
	private transient TransactionUtilService transactionUtilService;

	@Autowired
	private transient SocketIoService socketIoService;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private transient UserReadSystemMessageRepository userReadRepository;

	/**
	 *
	 *
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Scheduled(cron = "*/10 * * * * *")
	public void deleteOldSystemMessages() {
		transactionUtilService.runWithRequired(() -> {
			List<SystemMessage> forDeleteMessages = repository
					.findByCreationDateLessThan(new Date(new Date().getTime() - (86400 * 1000 * 7)));
			if (!forDeleteMessages.isEmpty()) {
				forDeleteMessages.forEach(this::delete);
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
		systemMessage = save(systemMessage);
		TransactionUtil.doAfterCommit(this::emitChange);
		return systemMessage;
	}

	/**
	 *
	 * @param userId
	 * @return
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<SystemMessageUser> findReadByUser(int userId) {
		return findAll().stream().map(message -> translateToUser(message, userId)).collect(Collectors.toList());
	}

	/**
	 * Translates the specified system message to one object with the isRead
	 * property defined
	 *
	 * @param message
	 * @param userId
	 * @return
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
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
		TransactionUtil.doAfterCommit(() -> emitChangeToUser(user.getId()));
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
