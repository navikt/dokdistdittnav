package no.nav.dokdistdittnav.kafka;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfoForMinSideOgPrintForsendelse {
	private String minSideBestillingsId;
	private String minSideForsendelseId;
	private String printBestillingsId;
	private String printForsendelseId;
	private String mottakerId;
}