package no.nav.dokdistdittnav.consumer.rdist001;

import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.OppdaterVarselInfoRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilregistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OppdaterForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseResponse;

public interface AdministrerForsendelse {

	HentForsendelseResponse hentForsendelse(final String forsendelseId);

	void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelseRequest);

	void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfo);

	FinnForsendelseResponseTo finnForsendelse(final FinnForsendelseRequestTo finnForsendelseRequestTo);

	OpprettForsendelseResponse opprettForsendelse(final OpprettForsendelseRequest forsendelseRequestTo);

	void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse);
}
