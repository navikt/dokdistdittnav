package no.nav.dokdistdittnav.qdist010;

import static java.lang.String.format;

import no.nav.dokdistdittnav.qdist010.domain.DistribuerForsendelseTilDittNavTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class DistribuerForsendelseTilDittNavMapper {

	@Handler
	public DistribuerForsendelseTilDittNavTo map(DistribuerTilKanal distribuerTilKanal) {
		return DistribuerForsendelseTilDittNavTo.builder()
				.forsendelseId(distribuerTilKanal.getForsendelseId())
				.build();
	}
}
