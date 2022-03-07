package no.nav.dokdistdittnav.consumer.rdist001;

import no.nav.dokdistdittnav.consumer.rdist001.to.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseResponseTo;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AdministrerForsendelse {

	HentForsendelseResponseTo hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus);

	void oppdaterForsendelseAndVarselStatus(String forsendelseId, String forsendelseStatus, String varselStatus);

	void oppdaterVarselStatus(String forsendelseId, String varselStatus);

	FinnForsendelseResponseTo finnForsendelse(final FinnForsendelseRequestTo finnForsendelseRequestTo);

	PersisterForsendelseResponseTo persisterForsendelse(final PersisterForsendelseRequestTo forsendelseRequestTo);

	void feilregistrerForsendelse(FeilRegistrerForsendelseRequest feilRegistrerForsendelse);
}
