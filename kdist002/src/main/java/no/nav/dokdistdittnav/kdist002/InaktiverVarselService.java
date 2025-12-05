package no.nav.dokdistdittnav.kdist002;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import static no.nav.dokdistdittnav.kafka.InaktiverVarselMapper.mapInaktiverVarsel;

@Slf4j
@Service
public class InaktiverVarselService {

	private final DokdistdittnavProperties properties;
	private final KafkaEventProducer kafkaEventProducer;

	public InaktiverVarselService(DokdistdittnavProperties properties,
								  KafkaEventProducer kafkaEventProducer) {
		this.properties = properties;
		this.kafkaEventProducer = kafkaEventProducer;
	}

	@Handler
	public void sendInaktiverVarselEventTilMinSide(String varselId) {
		kafkaEventProducer.publish(properties.getMinside().getVarseltopic(), varselId, mapInaktiverVarsel(varselId));
		log.info("Har sendt inaktivering av varsel for varselId (bestillingsId)={}", varselId);
	}

}