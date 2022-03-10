package no.nav.dokdistdittnav.kafka;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoneEventRequest {
	private String bestillingsId;
	private String forsendelseId;
	private String mottakerId;
}
