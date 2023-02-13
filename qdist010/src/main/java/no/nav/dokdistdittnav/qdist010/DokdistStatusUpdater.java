package no.nav.dokdistdittnav.qdist010;

import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.BEKREFTET;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatus.OPPRETTET;

@Component
public class DokdistStatusUpdater {

	private final AdministrerForsendelse administrerForsendelse;

	public DokdistStatusUpdater(AdministrerForsendelse administrerForsendelse) {
		this.administrerForsendelse = administrerForsendelse;
	}

	@Handler
	public void doUpdate(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);
		administrerForsendelse.oppdaterForsendelseAndVarselStatus(forsendelseId, BEKREFTET.name(), OPPRETTET.name());
	}
}
