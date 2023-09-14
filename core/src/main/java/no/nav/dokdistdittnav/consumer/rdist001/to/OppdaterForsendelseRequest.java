package no.nav.dokdistdittnav.consumer.rdist001.to;

import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatusCode;

public record OppdaterForsendelseRequest(
		Long forsendelseId,
		String forsendelseStatus,
		VarselStatusCode varselStatus) {
}
