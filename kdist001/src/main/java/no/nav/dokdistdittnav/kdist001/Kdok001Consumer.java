package no.nav.dokdistdittnav.kdist001;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.metrics.Monitor;
import no.nav.safselvbetjening.schemas.HoveddokumentLest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdittnav.constants.DomainConstants.DOK_KDOK001_CONSUMER;
import static no.nav.dokdistdittnav.utils.MDCGenerate.generateNewCallId;

@Slf4j
@Component
public class Kdok001Consumer {

	private final ObjectMapper objectMapper;
	private final Ferdigprodusent ferdigprodusent;

	public Kdok001Consumer(Ferdigprodusent ferdigprodusent) {
		this.ferdigprodusent = ferdigprodusent;
		this.objectMapper = new ObjectMapper();
	}

	@KafkaListener(
			topics = "${dokdistdittnav.topic.lestavmottaker}",
			containerFactory = "kafkaListenerContainerFactory",
			groupId = "dokdistdittnav-kdok001"
	)
	@Monitor(value = DOK_KDOK001_CONSUMER, createErrorMetric = true)
	public void onMesage(final ConsumerRecord<String, Object> record) {
		generateNewCallId(record.key());
		log.info("Innkommende lestavmottaker melding fra topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset());
		try {
			HoveddokumentLest hoveddokumentLest = objectMapper.readValue(record.value().toString(), HoveddokumentLest.class);
			log.info("Hentet lest av mottaker journalpostId={} fra topic={}", hoveddokumentLest.getJournalpostId(), record.topic());
			ferdigprodusent.updateVarselStatus(hoveddokumentLest);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
