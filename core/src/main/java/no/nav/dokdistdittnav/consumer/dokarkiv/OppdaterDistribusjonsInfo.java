package no.nav.dokdistdittnav.consumer.dokarkiv;

import java.time.OffsetDateTime;

public record OppdaterDistribusjonsInfo(
		boolean settStatusEkspedert,
		OffsetDateTime datoLest
) {
	public OppdaterDistribusjonsInfo(OffsetDateTime datoLest) {
		this(false, datoLest);
	}
}
