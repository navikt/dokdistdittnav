package no.nav.dokdistdittnav.consumer.rdist001.to;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinnForsendelseRequestTo {
	private String oppslagsNoekkel;
	private String verdi;
}
