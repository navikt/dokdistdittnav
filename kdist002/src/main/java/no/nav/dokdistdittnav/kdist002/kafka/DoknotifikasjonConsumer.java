package no.nav.dokdistdittnav.kdist002.kafka;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.alias.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.JsonParserTechnicalException;
import no.nav.dokdistdittnav.metrics.Monitor;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdittnav.constants.DomainConstants.DOK_KDOK001_CONSUMER;
import static no.nav.dokdistdittnav.utils.MDCGenerate.generateNewCallId;

@Slf4j
@Component
public class DoknotifikasjonConsumer {

	private final DokdistdittnavProperties properties;
	private final ObjectMapper objectMapper;

	@Autowired
	public DoknotifikasjonConsumer(DokdistdittnavProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(
			topics = "${dokdistdittnav.doknotifikasjon.status}",
			containerFactory = "kafkaListenerContainerFactory"
	)
	@Monitor(value = DOK_KDOK001_CONSUMER, createErrorMetric = true)
	public void onMesage(final ConsumerRecord<String, Object> record) {
		generateNewCallId(record.key());
		log.info("Innkommende lestavmottaker melding fra topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());
		try {
			objectMapper.readValue(record.value().toString(), DoknotifikasjonStatus.class);

		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Kunne ikke deserialisere avienPayload med feilmelding:" + e.getMessage(), e);
		}
	}
}
