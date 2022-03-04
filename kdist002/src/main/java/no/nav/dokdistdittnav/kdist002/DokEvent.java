package no.nav.dokdistdittnav.kdist002;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokEvent {
	private String eventType;
	private String appnavn;    // appnavn applikasjonen navn som henter fra kafka evnet.
	private String eventId;   // det er forsendelsens bestillingsId
}
