package no.nav.dokdistdittnav.consumer.rdist001.to;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OppdaterForsendelseRequest {
	private Long forsendelseId;
	private String forsendelseStatus;
	private VarselStatusCode varselStatus;
}
