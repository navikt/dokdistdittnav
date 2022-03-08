package no.nav.dokdistdittnav.kdist002;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.kafka.CamelKafkaProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.kafka.BrukerNotifikasjonMapper;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.dokdistdittnav.kdist002.metrics.MDCProcessor;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.DefaultKafkaManualCommit;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.KDIST002_ID;
import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.component.kafka.KafkaConstants.MANUAL_COMMIT;

@Slf4j
@Component
public class Kdist002Route extends RouteBuilder {

	private static final String QDIST009 = "qdist009";
	private static final String DONE_EVENT = "doknotifikasjon_done";

	private final CamelKafkaProperties camelKafkaProperties;
	private final Kdist002Service kdist002Service;
	private final DokdistdittnavProperties dittnavProperties;
	private final Queue qdist009;
	private final BrukerNotifikasjonMapper mapper;
	private final DoneEventProducer doneEventProducer;

	@Autowired
	public Kdist002Route(CamelKafkaProperties camelKafkaProperties, Kdist002Service kdist002Service,
						 DokdistdittnavProperties dittnavProperties, Queue qdist009, DoneEventProducer doneEventProducer) {
		this.camelKafkaProperties = camelKafkaProperties;
		this.kdist002Service = kdist002Service;
		this.dittnavProperties = dittnavProperties;
		this.qdist009 = qdist009;
		this.mapper = new BrukerNotifikasjonMapper();
		this.doneEventProducer = doneEventProducer;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.onExceptionOccurred(exchange -> {
					Throwable exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
					if (exception != null) {
						DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
						manual.getConsumer().seek(manual.getPartition(), manual.getRecordOffset());
						log.error("Kdist002 Teknisk feil. Seek tilbake til record(topic={}, partition={}, offset={})", manual.getTopicName(),
								manual.getPartition().partition(), manual.getRecordOffset());
					}
				})
				.retryAttemptedLogLevel(LoggingLevel.ERROR)
				.logRetryStackTrace(false)
				.logExhaustedMessageBody(false)
				.loggingLevel(LoggingLevel.ERROR));

		onException(AbstractDokdistdittnavFunctionalException.class)
				.handled(true)
				.maximumRedeliveries(0)
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(false)
				.logRetryAttempted(false)
				.process(exchange -> {
					DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
					if (manual != null) {
						log.error("Kdist002 Funksjonell feil i record" + defaultKafkaManualCommit(exchange));
						manual.commitSync();
					}
				})
				.log(LoggingLevel.WARN, log, "${exception}");


		from(camelKafkaProperties.buildKafkaUrl(dittnavProperties.getDoknotifikasjon().getStatustopic(), camelKafkaProperties.kafkaConsumer()))
				.id(KDIST002_ID)
				.process(new MDCProcessor())
				.process(exchange -> {
					DefaultKafkaManualCommit kafkaManualCommit = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
					log.info("Kdist002 mottatt " + defaultKafkaManualCommit(exchange));
				})
				.bean(kdist002Service)
				.process(exchange -> {
					DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
					DoneEventRequest doneEventRequest = exchange.getIn().getBody(DoneEventRequest.class);
					if (doneEventRequest != null) {
						exchange.setProperty(PROPERTY_FORSENDELSE_ID, doneEventRequest.getForsendelseId());
						exchange.setProperty(PROPERTY_BESTILLINGS_ID, doneEventRequest.getBestillingsId());
					}
					if (manual != null) {
						log.info("Kdist002, manual commit " + defaultKafkaManualCommit(exchange));
					}
				})
				.choice()
				.when(simple("${body}").isNull())
					.process(exchange -> log.info("Avsluttet behandlingen: " + defaultKafkaManualCommit(exchange)))
					.endChoice()
				.otherwise()
					.multicast()
						.to("direct:" + QDIST009)
						.to("direct:" + DONE_EVENT)
				.end();

		from("direct:" + QDIST009)
				.id(QDIST009)
				.setExchangePattern(InOnly)
				.process(exchange -> {
					DoneEventRequest doneEventRequest = exchange.getIn().getBody(DoneEventRequest.class);
					DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();
					distribuerTilKanal.setForsendelseId(doneEventRequest.getForsendelseId());
					exchange.getIn().setBody(distribuerTilKanal);
				})
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
				.to("jms:" + qdist009.getQueueName())
				.log(INFO, log, "Kdist002 skrevet forsendelse med " + getIdsForLogging() + " på kø QDIST009.")
				.end();

		from("direct:" + DONE_EVENT)
				.id(DONE_EVENT)
				.process(exchange -> {
					new MDCProcessor();
					DoneEventRequest doneEventRequest = exchange.getIn().getBody(DoneEventRequest.class);
					exchange.getIn().setHeader(KafkaConstants.KEY, mapper.mapNokkelForKdist002(doneEventRequest, dittnavProperties.getAppnavn()));
					exchange.getIn().setBody(mapper.mapDoneInput());
				})
				.to(camelKafkaProperties.buildKafkaUrl(dittnavProperties.getBrukernotifikasjon().getTopicdone(),
						camelKafkaProperties.kafkaProducer()))
				.log(INFO, "Kdist002 skrevet hendelse med " + getIdsForLogging() + " til topic=" + dittnavProperties.getBrukernotifikasjon().getTopicdone())
				.end();
	}

	private static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}

	private String defaultKafkaManualCommit(Exchange exchange) {
		DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		return format("(topic=%s, partition={%s, offset=%s, groupId=%s).", manual.getTopicName(), manual.getPartition().partition(), manual.getRecordOffset(), camelKafkaProperties.getGroupId());
	}
}
