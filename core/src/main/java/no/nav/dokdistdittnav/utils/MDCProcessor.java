package no.nav.dokdistdittnav.utils;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import java.util.UUID;

import static java.util.Objects.isNull;
import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class MDCProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		setOrGenerateCallIdToMdc(exchange);
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
}
