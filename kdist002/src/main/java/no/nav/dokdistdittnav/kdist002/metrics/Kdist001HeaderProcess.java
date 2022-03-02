package no.nav.dokdistdittnav.kdist002.metrics;

import no.nav.dokdistdittnav.constants.MdcConstants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import java.util.UUID;

import static java.util.Objects.isNull;
import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistdittnav.constants.MdcConstants.NAV_CONSUMER_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class Kdist001HeaderProcess implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		setOrGenerateCallIdToMdc(exchange);
		setConsumerIdToMdc(exchange);
	}

	public static void setOrGenerateCallIdToMdc(Exchange exchange) {
		final String callId = exchange.getIn().getHeader(CALL_ID, String.class);
		if (isNull(callId) || isBlank(callId)) {
			String newCallId = UUID.randomUUID().toString();
			exchange.getIn().setHeader(CALL_ID, newCallId);
			MDC.put(CALL_ID, newCallId);
		} else {
			MDC.put(CALL_ID, callId);
		}
	}

	public static void setConsumerIdToMdc(Exchange exchange) {
		String consumerId = exchange.getIn().getHeader(NAV_CONSUMER_ID, String.class);
		if (!isBlank(consumerId)) {
			MDC.put(NAV_CONSUMER_ID, consumerId);
		}
	}
}
