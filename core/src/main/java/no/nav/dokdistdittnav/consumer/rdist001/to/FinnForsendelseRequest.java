package no.nav.dokdistdittnav.consumer.rdist001.to;

import lombok.Builder;
import lombok.Data;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.Oppslagsnoekkel;

@Data
@Builder
public class FinnForsendelseRequest {
	private Oppslagsnoekkel oppslagsnoekkel;
	private String verdi;
}
