package no.nav.dokdistdittnav.kdist002.kafka;

import no.nav.dokdistdittnav.kdist002.DokEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.junit.jupiter.api.Assertions.*;

class DoknotifikasjonConsumerTest {


	@Test
	public void testString() {
		String s = "B-dokdistdittnav-16a80b3e-47a5-49ed-a02d-6b37c7261f17";
		DokEvent dokEvent = dokEvent(s);
		assertNotNull(dokEvent);
	}

	private DokEvent dokEvent(String bestillingId) {
		String eventId = substring(bestillingId, bestillingId.length() - UUID.randomUUID().toString().length());
		String appnavn = substringAfter(substring(bestillingId, 0,bestillingId.length() - 37), "-");
		String eventtype = substringBefore(bestillingId,"-");
		return DokEvent.builder()
				.eventType(eventtype)
				.appnavn(appnavn)
				.eventId(eventId)
				.build();
	}

}