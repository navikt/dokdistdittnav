package no.nav.dokdistdittnav.consumer.rdist001.to;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinnForsendelseRequest {
	private String oppslagsnoekkel;
	private String verdi;
}
