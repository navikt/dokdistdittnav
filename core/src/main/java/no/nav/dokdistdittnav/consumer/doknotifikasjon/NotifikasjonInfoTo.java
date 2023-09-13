package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import java.time.LocalDateTime;
import java.util.Set;

public record NotifikasjonInfoTo(
		int id,
		String bestillerId,
		String status,
		int antallRenotifikasjoner,
		Set<NotifikasjonDistribusjonDto> notifikasjonDistribusjoner
) {

	public record NotifikasjonDistribusjonDto(
			int id,
			String status,
			String kanal,
			String kontaktInfo,
			String tittel,
			String tekst,
			LocalDateTime sendtDato) {
	}
}
