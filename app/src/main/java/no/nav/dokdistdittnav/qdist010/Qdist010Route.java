package no.nav.dokdistdittnav.qdist010;

import static org.apache.camel.LoggingLevel.ERROR;

import no.nav.dokdistdittnav.constants.MdcConstants;
import no.nav.dokdistdittnav.exception.functional.AbstractDokdistdittnavFunctionalException;
import no.nav.dokdistdittnav.metrics.Qdist010MetricsRoutePolicy;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class Qdist010Route extends SpringRouteBuilder {

	public static final String SERVICE_ID = "qdist010";
	static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";

	private final Qdist010Service qdist010Service;
	private final DistribuerForsendelseTilDittNavValidatorAndMapper distribuerForsendelseTilDittNavValidatorAndMapper;
	private final DokdistStatusUpdater dokdistStatusUpdater;
	private final Queue qdist010;
	private final Queue qdist010FunksjonellFeil;
	private final Qdist010MetricsRoutePolicy qdist010MetricsRoutePolicy;

	@Inject
	public Qdist010Route(Qdist010Service qdist010Service,
						 DistribuerForsendelseTilDittNavValidatorAndMapper distribuerForsendelseTilDittNavValidatorAndMapper,
						 DokdistStatusUpdater dokdistStatusUpdater,
						 Queue qdist010,
						 Queue qdist010FunksjonellFeil,
						 Qdist010MetricsRoutePolicy qdist010MetricsRoutePolicy) {
		this.qdist010Service = qdist010Service;
		this.distribuerForsendelseTilDittNavValidatorAndMapper = distribuerForsendelseTilDittNavValidatorAndMapper;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
		this.qdist010 = qdist010;
		this.qdist010FunksjonellFeil = qdist010FunksjonellFeil;
		this.qdist010MetricsRoutePolicy = qdist010MetricsRoutePolicy;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(true)
				.loggingLevel(ERROR));

		onException(AbstractDokdistdittnavFunctionalException.class, JAXBException.class)
				.handled(true)
				.useOriginalMessage()
				.log(LoggingLevel.WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist010FunksjonellFeil.getQueueName());

		from("jms:" + qdist010.getQueueName() +
				"?transacted=true")
				.routeId(SERVICE_ID)
				.routePolicy(qdist010MetricsRoutePolicy)
				.setExchangePattern(ExchangePattern.InOnly)
				.doTry()
				.setProperty(PROPERTY_BESTILLINGS_ID, simple("${in.header.callId}", String.class))
				.setProperty(PROPERTY_FORSENDELSE_ID, xpath("//forsendelseId/text()", String.class))
				.log(LoggingLevel.INFO, log, "qdist010 har mottatt forsendelse med " + getIdsForLogging())
				.process(exchange -> MDC.put(MdcConstants.CALL_ID, (String) exchange.getProperty(PROPERTY_BESTILLINGS_ID)))
				.doCatch(Exception.class)
				.end()
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelseTilSentralPrint.class)))
				.bean(distribuerForsendelseTilDittNavValidatorAndMapper)
				.bean(qdist010Service)
				//todo: Opprett dokumenthenvendelse via melding:virksomhet:opprettDokumenthenvendelse_v1 (mq)
				//todo: Bestill varselutsending (mq)
				.bean(dokdistStatusUpdater);
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
