package no.nav.dokdistdittnav.qdist010;

import static no.nav.dokdistdittnav.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistdittnav.qdist010.Qdist010Route.PROPERTY_FORSENDELSE_ID;

import no.nav.dokdistdittnav.exception.functional.ForsendelseManglerForsendelseIdFunctionalException;
import no.nav.dokdistdittnav.exception.functional.ForsendelseManglerPaakrevdHeaderFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.MDC;

/**
 * @author Erik Bråten, Visma Consulting.
 */
public class IdsProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        setBestillingsIdAsPropertyAndAddCallIdToMdc(exchange);
        setForsendelseIdAsProperty(exchange);
    }

    private void setBestillingsIdAsPropertyAndAddCallIdToMdc(Exchange exchange) {
        final String callId = exchange.getIn().getHeader(CALL_ID, String.class);
        if (callId == null) {
            throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist010 har mottatt forsendelse uten påkrevd header callId");
        } else if (callId.trim().isEmpty()) {
            throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist010 har mottatt forsendelse med tom header callId");
        }
        MDC.put(CALL_ID, callId);
    }

    private void setForsendelseIdAsProperty(Exchange exchange) {
        String forsendelseId = XPathBuilder.xpath("//forsendelseId/text()").evaluate(exchange, String.class);
        if (forsendelseId == null) {
            throw new ForsendelseManglerForsendelseIdFunctionalException("qdist010 har mottatt forsendelse uten påkrevd forsendelseId");
        } else if (forsendelseId.trim().isEmpty()) {
            throw new ForsendelseManglerForsendelseIdFunctionalException("qdist010 har mottatt forsendelse med tom forsendelseId");
        }
        exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);
    }
}
