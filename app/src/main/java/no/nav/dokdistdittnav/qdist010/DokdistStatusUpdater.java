package no.nav.dokdistdittnav.qdist010;

import static no.nav.dokdistdittnav.constants.DomainConstants.FORSENDELSE_STATUS_EKSPEDERT;
import static no.nav.dokdistdittnav.qdist010.Qdist010Route.PROPERTY_FORSENDELSE_ID;

import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class DokdistStatusUpdater {

	private final AdministrerForsendelse administrerForsendelse;

	public DokdistStatusUpdater(AdministrerForsendelse administrerForsendelse) {
		this.administrerForsendelse = administrerForsendelse;
	}

	@Handler
	public void doUpdate(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);
		administrerForsendelse.oppdaterForsendelseStatus(forsendelseId, FORSENDELSE_STATUS_EKSPEDERT);
	}

}
