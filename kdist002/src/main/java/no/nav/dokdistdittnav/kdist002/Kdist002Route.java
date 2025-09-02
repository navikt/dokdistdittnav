package no.nav.dokdistdittnav.kdist002;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBContext;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.config.kafka.CamelKafkaProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.dokdistdittnav.utils.MDCProcessor;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.consumer.DefaultKafkaManualCommit;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistdittnav.constants.DomainConstants.KDIST002_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_DITTNAV_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_DITTNAV_FEILET_FORSENDELSE_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_PRINT_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_PRINT_FORSENDELSE_ID;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;
import static org.apache.camel.component.kafka.KafkaConstants.MANUAL_COMMIT;

@Slf4j
@Component
public class Kdist002Route extends RouteBuilder {

	private static final String QDIST009 = "qdist009";
	private static final String DONE_EVENT = "doknotifikasjon_done";
	public static final String TMS_EKSTERN_VARSLING = "tms-ekstern-varsling";

	private final CamelKafkaProperties camelKafkaProperties;
	private final Kdist002Service kdist002Service;
	private final DokdistdittnavProperties dokdistdittnavProperties;
	private final Queue qdist009;
	private final DoneEventProducer doneEventProducer;

	public Kdist002Route(CamelKafkaProperties camelKafkaProperties,
						 Kdist002Service kdist002Service,
						 DokdistdittnavProperties dokdistdittnavProperties,
						 Queue qdist009,
						 DoneEventProducer doneEventProducer) {
		this.camelKafkaProperties = camelKafkaProperties;
		this.kdist002Service = kdist002Service;
		this.dokdistdittnavProperties = dokdistdittnavProperties;
		this.qdist009 = qdist009;
		this.doneEventProducer = doneEventProducer;
	}

	@Override
	public void configure() throws Exception {

		//@formatter:off
		errorHandler(defaultErrorHandler()
				.onExceptionOccurred(exchange -> {
					Throwable exception = exchange.getProperty(EXCEPTION_CAUGHT, Throwable.class);
					if (exception != null && !(exception instanceof AbstractDokdistdittnavFunctionalException)) {
						DefaultKafkaManualCommit manual = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
						manual.getCamelExchangePayload().consumer.seek(manual.getPartition(), manual.getRecordOffset());

						log.error("Kdist002 Teknisk feil. Seek tilbake til record(topic={}, partition={}, offset={})",
								manual.getTopicName(), manual.getPartition().partition(), manual.getRecordOffset(), exception);
					}
				})
				.retryAttemptedLogLevel(ERROR)
				.logRetryStackTrace(false)
				.logExhaustedMessageBody(false)
				.loggingLevel(ERROR));

		onException(AbstractDokdistdittnavFunctionalException.class)
				.handled(true)
				.maximumRedeliveries(0)
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(false)
				.logRetryAttempted(false)
				.process(this::defaultKafkaManualCommit)
				.log(WARN, log, "${exception}");


		from(camelKafkaProperties.buildKafkaUrl(dokdistdittnavProperties.getDoknotifikasjon().getStatustopic(), camelKafkaProperties.kafkaConsumer()))
				.autoStartup(dokdistdittnavProperties.isAutostartup())
				.id(KDIST002_ID)
				.process(new MDCProcessor())
				.choice()
					.when(PredicateBuilder.and(
							simple("${body.bestillerId}").isNotEqualTo(dokdistdittnavProperties.getAppnavn()),
							simple("${body.bestillerId}").isNotEqualTo(TMS_EKSTERN_VARSLING)))
						.process(this::defaultKafkaManualCommit)
					.otherwise()
						.bean(kdist002Service)
						.choice()
							.when(simple("${body}").isNull())
								.process(this::defaultKafkaManualCommit)
							.otherwise()
								.process(exchange -> {
									DoneEventRequest doneEventRequest = exchange.getIn().getBody(DoneEventRequest.class);
									exchange.setProperty(PROPERTY_DITTNAV_FEILET_FORSENDELSE_ID, doneEventRequest.getDittnavFeiletForsendelseId());
									exchange.setProperty(PROPERTY_PRINT_FORSENDELSE_ID, doneEventRequest.getPrintForsendelseId());
									exchange.setProperty(PROPERTY_DITTNAV_BESTILLINGS_ID, doneEventRequest.getDittnavBestillingsId());
									exchange.setProperty(PROPERTY_PRINT_BESTILLINGS_ID, doneEventRequest.getPrintBestillingsId());
								})
								.multicast()
									.to("direct:" + QDIST009)
									.to("direct:" + DONE_EVENT)
								.end()
						.end()
				.end()
				.process(this::defaultKafkaManualCommit);

		from("direct:" + QDIST009)
				.id(QDIST009)
				.process(exchange -> {
					DoneEventRequest doneEventRequest = exchange.getIn().getBody(DoneEventRequest.class);
					DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();
					distribuerTilKanal.setForsendelseId(doneEventRequest.getPrintForsendelseId());
					exchange.getIn().setBody(distribuerTilKanal);
				})
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.convertBodyTo(String.class, UTF_8.toString())
				.to("jms:" + qdist009.getQueueName())
				.log(INFO, log, "Kdist002 skrevet forsendelse med " + getIdsForLoggingPrint() + " på kø QDIST009.")
				.end();

		from("direct:" + DONE_EVENT)
				.id(DONE_EVENT)
				.bean(doneEventProducer)
				.log(INFO, "Kdist002 skrevet hendelse med " + getIdsForLoggingDittnav() + " til topic=" + dokdistdittnavProperties.getBrukernotifikasjon().getTopicdone())
				.end();
		//@formatter:on
	}

	private static String getIdsForLoggingDittnav() {
		return "dittnavBestillingsId=${exchangeProperty." + PROPERTY_DITTNAV_BESTILLINGS_ID + "}, " +
			   "dittnavFeiletForsendelseId=${exchangeProperty." + PROPERTY_DITTNAV_FEILET_FORSENDELSE_ID + "}";
	}

	private static String getIdsForLoggingPrint() {
		return "printBestillingsId=${exchangeProperty." + PROPERTY_PRINT_BESTILLINGS_ID + "}, " +
			   "printForsendelseId=${exchangeProperty." + PROPERTY_PRINT_FORSENDELSE_ID + "}";
	}

	private void defaultKafkaManualCommit(Exchange exchange) {
		DefaultKafkaManualCommit manualCommit = exchange.getIn().getHeader(MANUAL_COMMIT, DefaultKafkaManualCommit.class);
		if (manualCommit != null) {
			if (log.isDebugEnabled()) {
				log.debug("Kdist002 manual commit {}", createLogging(manualCommit));
			}
			manualCommit.commit();
		}
	}

	private String createLogging(DefaultKafkaManualCommit manualCommit) {
		return format("(topic=%s, partition={%s, offset=%s, groupId=%s).", manualCommit.getTopicName(), manualCommit.getPartition().partition(), manualCommit.getRecordOffset(), camelKafkaProperties.getGroupId());
	}
}
