package no.nav.dokdistdittnav.consumer.rdist001;

import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.OppdaterVarselInfoRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseResponse;

public interface AdministrerForsendelse {

	HentForsendelseResponse hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus);

	void oppdaterForsendelseAndVarselStatus(String forsendelseId, String forsendelseStatus, String varselStatus);

	void oppdaterVarselStatus(String forsendelseId, String varselStatus);

	void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfo);

	FinnForsendelseResponseTo finnForsendelse(final FinnForsendelseRequestTo finnForsendelseRequestTo);

	OpprettForsendelseResponse opprettForsendelse(final OpprettForsendelseRequest forsendelseRequestTo);

	void feilregistrerForsendelse(FeilRegistrerForsendelseRequest feilRegistrerForsendelse);
}
