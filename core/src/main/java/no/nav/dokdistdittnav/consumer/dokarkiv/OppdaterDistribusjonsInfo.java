package no.nav.dokdistdittnav.consumer.dokarkiv;

import java.time.OffsetDateTime;

public record OppdaterDistribusjonsInfo(
		OffsetDateTime datoLest
) {
	public static final boolean settStatusEkspedert = false;
}
