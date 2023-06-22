package no.nav.dokdistdittnav.consumer.rdist001;

import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.OppdaterVarselInfoRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilregistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OppdaterForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseResponse;

public interface AdministrerForsendelse {

	HentForsendelseResponse hentForsendelse(final String forsendelseId);

	void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelseRequest);

	void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfo);

	String finnForsendelse(final FinnForsendelseRequest finnForsendelseRequest);

	OpprettForsendelseResponse opprettForsendelse(final OpprettForsendelseRequest forsendelseRequestTo);

	void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse);
}
