package no.nav.dokdistdittnav.kdist002.kafka;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.functional.JsonParserTechnicalException;
import no.nav.dokdistdittnav.kdist002.DokEvent;
import no.nav.dokdistdittnav.metrics.Monitor;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdittnav.constants.DomainConstants.BESTILLING_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.DOK_KDOK001_CONSUMER;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatus.OPPRETTET;
import static no.nav.dokdistdittnav.kdist002.utils.NotifikasjonStatus.FEILET;
import static no.nav.dokdistdittnav.utils.MDCGenerate.generateNewCallId;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

@Slf4j
@Component
public class DoknotifikasjonConsumer {

	private static final String OPPGAVE_PREFIX = "0";
	private static final String BESKJED_PREFIX = "B";
	private final DokdistdittnavProperties properties;
	private final ObjectMapper objectMapper;
	private final AdministrerForsendelseConsumer administrerForsendelse;

	@Autowired
	public DoknotifikasjonConsumer(DokdistdittnavProperties properties, ObjectMapper objectMapper,
								   AdministrerForsendelseConsumer administrerForsendelse) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.administrerForsendelse = administrerForsendelse;
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

	//example for call_id i doknotifikasjon

	private boolean erBestillerIdDittnav(DoknotifikasjonStatus doknotifikasjonStatus) {
		return !properties.getAppnavn().equals(doknotifikasjonStatus.getBestillerId()) && FEILET.name().equals(doknotifikasjonStatus.getStatus());
	}

	private boolean erOpprettetVarselStatus(String varselStatus) {
		return OPPRETTET.name().equals(varselStatus);
	}

	private HentForsendelseResponseTo hentForsendelse(String bestillingId) {
		FinnForsendelseResponseTo finnForsendelseResponseTo = administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(BESTILLING_ID)
				.verdi(bestillingId)
				.build());

	return 	administrerForsendelse.hentForsendelse(requireNonNull(finnForsendelseResponseTo.getForsendelseId(),
			"ForsendelseId kan ikke være null"));


	}

	private DokEvent extractValueFraBestillingIdEvent(String bestillingId) {
		String eventId = substring(bestillingId, bestillingId.length() - 36);
		String appnavn = substringAfter(substring(bestillingId, 0,bestillingId.length() - 37), "-");
		String eventtype = substringBefore(bestillingId,"-");

		return DokEvent.builder()
				.eventType(eventtype)
				.appnavn(appnavn)
				.eventId(eventId)
				.build();
	}
}
