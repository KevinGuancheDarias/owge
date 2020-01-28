/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.owgejava.configurations.SqsConfig;
import com.kevinguanchedarias.owgejava.enumerations.OwgeSqsMessageEnum;
import com.kevinguanchedarias.owgejava.functional.SqsMessageHandler;
import com.kevinguanchedarias.owgejava.pojo.OwgeSqsMessage;
import com.kevinguanchedarias.sqs.JsonMessageInner;
import com.kevinguanchedarias.sqs.JsonMessageOuter;
import com.kevinguanchedarias.sqs.Message;
import com.kevinguanchedarias.sqs.MessageBuilder;
import com.kevinguanchedarias.sqs.consumer.Consumer;
import com.kevinguanchedarias.sqs.consumer.JsonConsumer;
import com.kevinguanchedarias.sqs.producer.JsonProducer;
import com.kevinguanchedarias.sqs.producer.Producer;

/**
 * 
 * @since 0.8.0
 * @deprecated While SQS was a nice idea, has too many errors in concurrent
 *             "stressful" servers, while should be fixed, SQS is a low near-to
 *             none priority project
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Deprecated(since = "0.8.1")
public class SqsManagerService {
	private static final Logger LOG = Logger.getLogger(SqsManagerService.class);

	@Autowired
	private SqsConfig sqsConfig;

	private ObjectMapper mapper = new ObjectMapper();
	private Producer<JsonMessageInner> producer = new JsonProducer(mapper);
	private Consumer<JsonMessageInner> consumer = new JsonConsumer(mapper);
	private Map<OwgeSqsMessageEnum, SqsMessageHandler> handlers = new EnumMap<>(OwgeSqsMessageEnum.class);

	/**
	 * Adds a new handler
	 * 
	 * @param type
	 * @param handler
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void addHandler(OwgeSqsMessageEnum type, SqsMessageHandler handler) {
		if (handlers.get(type) != null) {
			LOG.warn("Notice, overwriting handler for " + type);
		}
		handlers.put(type, handler);
	}

	/**
	 * Sends a message
	 * 
	 * @param messageData         the Object message, will be serialized to JSON
	 * @param deliverAfterSeconds Time to wait in seconds before the message should
	 *                            arrive
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void sendMessage(OwgeSqsMessage messageData, Long deliverAfterSeconds) {
		LOG.debug("Sending SQS message");
		producer.sendMessageSync(createJsonMessage(messageData, deliverAfterSeconds));
	}

	/**
	 * 
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostConstruct
	public void init() {
		String host = sqsConfig.getHost();
		Integer port = sqsConfig.getPort();
		String queue = sqsConfig.getQueue();
		producer.connect(host, port, queue);
		consumer.connect(host, port, queue);
	}

	/**
	 * Start consuming messages after all beans are ready
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@EventListener
	public void listenMessages(ContextRefreshedEvent event) {
		LOG.info("Start listening to SQS events");
		consumer.onMessage(this::handleMessage);
	}

	private void handleMessage(Message<JsonMessageInner> message) {
		OwgeSqsMessageEnum type = detectMessageType(message.getBody().getType());
		SqsMessageHandler action = handlers.get(type);
		if (action != null) {
			action.handle(new OwgeSqsMessage(type, message.getBody().getContent()));
		} else {
			LOG.warn("No handler for SQS message type " + type + " with content: " + message.getBody().getContent());
		}
	}

	private OwgeSqsMessageEnum detectMessageType(String type) {
		if (type == null) {
			LOG.warn("The SQS message doesn't have a type " + type);
			return OwgeSqsMessageEnum.NOT_KNOWN;
		} else {
			return OwgeSqsMessageEnum.valueOf(type);
		}
	}

	private static JsonMessageOuter createJsonMessage(OwgeSqsMessage messageData, Long deliverAfterSeconds) {
		return MessageBuilder.newInstance(JsonMessageOuter.class)
				.withBody(new JsonMessageInner(messageData.getType().name(), messageData.getContent()))
				.withDeliverDelay(deliverAfterSeconds).build();
	}
}
