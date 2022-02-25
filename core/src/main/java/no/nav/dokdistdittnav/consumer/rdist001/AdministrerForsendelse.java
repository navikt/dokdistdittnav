package no.nav.dokdistdittnav.consumer.rdist001;

import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AdministrerForsendelse {

	HentForsendelseResponseTo hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(final String forsendelseId, final String forsendelseStatus, final String varselStatus);

	FinnForsendelseResponseTo finnForsendelse(final FinnForsendelseRequestTo finnForsendelseRequestTo);
}
