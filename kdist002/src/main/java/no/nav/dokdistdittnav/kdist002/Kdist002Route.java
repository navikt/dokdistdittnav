package no.nav.dokdistdittnav.kdist002;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.kafka.CamelKafkaProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.dokdistdittnav.metrics.DittnavMetricsRoutePolicy;
import no.nav.dokdistdittnav.utils.MDCProcessor;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.consumer.DefaultKafkaManualCommit;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistdittnav.constants.DomainConstants.KDIST002_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;
import static org.apache.camel.component.kafka.KafkaConstants.MANUAL_COMMIT;
import static org.apache.camel.support.builder.PredicateBuilder.or;

@Slf4j
@Component
public class Kdist002Route extends RouteBuilder {

	private static final String QDIST009 = "qdist009";
	private static final String DONE_EVENT = "doknotifikasjon_done";

	private final CamelKafkaProperties camelKafkaProperties;
	private final Kdist002Service kdist002Service;
	private final DokdistdittnavProperties dittnavProperties;
	private final Queue qdist009;
	private final DoneEventProducer doneEventProducer;
	private final DittnavMetricsRoutePolicy metricsRoutePolicy;

	@Autowired
	public Kdist002Route(CamelKafkaProperties camelKafkaProperties, Kdist002Service kdist002Service,
						 DokdistdittnavProperties dittnavProperties, Queue qdist009,
						 DoneEventProducer doneEventProducer, DittnavMetricsRoutePolicy metricsRoutePolicy) {
		this.camelKafkaProperties = camelKafkaProperties;
		this.kdist002Service = kdist002Service;
		this.dittnavProperties = dittnavProperties;
		this.qdist009 = qdist009;
		this.doneEventProducer = doneEventProducer;
		this.metricsRoutePolicy = metricsRoutePolicy;
	}

	@Override
	public void configure() throws Exception {

		//@formatter:off
		errorHandler(defaultErrorHandler()
				.retryAttemptedLogLevel(ERROR)
				.logRetryStackTrace(false)
				.logExhaustedMessageBody(false)
				.loggingLevel(ERROR));

		onException(AbstractDokdistdittnavTechnicalException.class)
				.process(exchange -> {
					DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
					manual.getCamelExchangePayload().consumer.seek(manual.getPartition(), manual.getRecordOffset());
					log.warn("Kdist002 Teknisk feil. Seek tilbake til record(topic={}, partition={}, offset={})", manual.getTopicName(),
							manual.getPartition().partition(), manual.getRecordOffset());
				})
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(false)
				.log(WARN, log, "${exception}");

		onException(AbstractDokdistdittnavFunctionalException.class)
				.handled(true)
				.maximumRedeliveries(0)
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(false)
				.logRetryAttempted(false)
				.process(this::defaultKafkaManualCommit)
				.log(WARN, log, "${exception}");

		from(camelKafkaProperties.buildKafkaUrl(dittnavProperties.getDoknotifikasjon().getStatustopic(), camelKafkaProperties.kafkaConsumer()))
				.id(KDIST002_ID)
				.process(new MDCProcessor())
				.process(exchange -> log.info("Kdist002 mottatt " + createLoggingFraHeader(exchange)))
				.routePolicy(metricsRoutePolicy)
				.choice()
				.when(or(simple("${body.bestillerId}").isNotEqualTo(dittnavProperties.getAppnavn()),
						simple("${body.status}").isNotEqualTo(FEILET.name())))
					.process(this::defaultKafkaManualCommit)
					.endChoice()
				.otherwise()
				.bean(kdist002Service)
					.choice()
					.when(simple("${body}").isNull())
						.process(this::defaultKafkaManualCommit)
						.endChoice()
					.otherwise()
						.process(exchange -> {
							DoneEventRequest doneEventRequest = exchange.getIn().getBody(DoneEventRequest.class);
							exchange.setProperty(PROPERTY_FORSENDELSE_ID, doneEventRequest.getForsendelseId());
							exchange.setProperty(PROPERTY_BESTILLINGS_ID, doneEventRequest.getBestillingsId());
						})
						.multicast()
						.to("direct:" + QDIST009)
						.to("direct:" + DONE_EVENT)
				.end()
				.process(this::defaultKafkaManualCommit);

		from("direct:" + QDIST009)
				.id(QDIST009)
				.process(exchange -> {
					DoneEventRequest doneEventRequest = exchange.getIn().getBody(DoneEventRequest.class);
					DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();
					distribuerTilKanal.setForsendelseId(doneEventRequest.getForsendelseId());
					exchange.getIn().setBody(distribuerTilKanal);
				})
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.convertBodyTo(String.class, UTF_8.toString())
				.to("jms:" + qdist009.getQueueName())
				.log(INFO, log, "Kdist002 skrevet forsendelse med " + getIdsForLogging() + " på kø QDIST009.")
				.end();

		from("direct:" + DONE_EVENT)
				.id(DONE_EVENT)
				.bean(doneEventProducer)
				.log(INFO, "Kdist002 skrevet hendelse med " + getIdsForLogging() + " til topic=" + dittnavProperties.getBrukernotifikasjon().getTopicdone())
				.end();
		//@formatter:on
	}

	private static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}

	private void defaultKafkaManualCommit(Exchange exchange) {
		DefaultKafkaManualCommit manualCommit = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		if (manualCommit != null) {
			log.info("Kdist002, manual commit " + createLogging(manualCommit));
			manualCommit.commit();
		}
	}

	private String createLoggingFraHeader(Exchange exchange) {
		DefaultKafkaManualCommit manualCommit = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		return createLogging(manualCommit);
	}

	private String createLogging(DefaultKafkaManualCommit manualCommit) {
		return format("(topic=%s, partition={%s, offset=%s, groupId=%s).", manualCommit.getTopicName(), manualCommit.getPartition().partition(), manualCommit.getRecordOffset(), camelKafkaProperties.getGroupId());
	}
}
