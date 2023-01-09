package no.nav.dokdistdittnav.kdist002;


import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper.mapDoneInput;
import static no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper.mapNokkelForKdist002;

@Slf4j
@Component
public class DoneEventProducer {

	private final DokdistdittnavProperties properties;
	private final KafkaEventProducer kafkaEventProducer;

	@Autowired
	public DoneEventProducer(DokdistdittnavProperties properties,
							 KafkaEventProducer kafkaEventProducer) {
		this.properties = properties;
		this.kafkaEventProducer = kafkaEventProducer;
	}

	@Handler
	public void sendDoneEvent(DoneEventRequest doneEventRequest) {
		NokkelInput nokkelInput = mapNokkelForKdist002(doneEventRequest, properties.getAppnavn());
		log.info("Kdist002 mottatt hendelse med eventId/bestillingsId={} til å skrive til (topic={})", doneEventRequest.getForsendelseId(), properties.getBrukernotifikasjon().getTopicdone());
		kafkaEventProducer.publish(properties.getBrukernotifikasjon().getTopicdone(), nokkelInput, mapDoneInput());
	}
}
