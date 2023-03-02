package no.nav.dokdistdittnav.kafka;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoneEventRequest {
	private String dittnavBestillingsId;
	private String dittnavFeiletForsendelseId;
	private String printBestillingsId;
	private String printForsendelseId;
	private String mottakerId;
}
