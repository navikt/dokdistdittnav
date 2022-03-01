package no.nav.dokdistdittnav.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.exception.technical.KafkaTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;

import static java.lang.Integer.MAX_VALUE;
import static no.nav.dokdistdittnav.constants.RetryConstants.DELAY_LONG;

@Slf4j
@Component
public class KafkaEventProducer {

	private static final String KAFKA_NOT_AUTHENTICATED = "Not authenticated to publish to topic: ";
	private static final String KAFKA_FAILED_TO_SEND = "Failed to send message to kafka. Topic: ";

	private final KafkaTemplate<Object, Object> kafkaTemplate;

	@Inject
	KafkaEventProducer(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") KafkaTemplate<Object, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Monitor(createErrorMetric = true, errorMetricInclude = KafkaTechnicalException.class)
	@Retryable(include = KafkaTechnicalException.class, maxAttempts = MAX_VALUE, backoff = @Backoff(delay = DELAY_LONG))
	public void publish(String topic, Object key, Object event) {
		ProducerRecord<Object, Object> producerRecord = new ProducerRecord(
				topic,
				null,
				System.currentTimeMillis(),
				key,
				event
		);

		try {
			SendResult<Object, Object> sendResult = kafkaTemplate.send(producerRecord).get();
			log.info("Hendelse skrevet til topic. Timestamp={}, partition={}, topic={}",
					sendResult.getRecordMetadata().timestamp(),
					sendResult.getRecordMetadata().partition(),
					sendResult.getRecordMetadata().topic()
			);
		} catch (ExecutionException executionException) {
			if (executionException.getCause() instanceof KafkaProducerException) {
				KafkaProducerException kafkaProducerException = (KafkaProducerException) executionException.getCause();
				if (kafkaProducerException.getCause() instanceof TopicAuthorizationException) {
					throw new KafkaTechnicalException(KAFKA_NOT_AUTHENTICATED + topic, kafkaProducerException.getCause());
				}
			}
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, executionException);
		} catch (InterruptedException | KafkaException e) {
			throw new KafkaTechnicalException(KAFKA_FAILED_TO_SEND + topic, e);
		}
	}
}
