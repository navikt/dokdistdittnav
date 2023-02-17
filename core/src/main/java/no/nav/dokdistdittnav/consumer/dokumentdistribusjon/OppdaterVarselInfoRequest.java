package no.nav.dokdistdittnav.consumer.dokumentdistribusjon;

import java.util.Set;

public record OppdaterVarselInfoRequest(
		String forsendelseId,
		Set<Notifikasjon> notifikasjoner
) {

}
