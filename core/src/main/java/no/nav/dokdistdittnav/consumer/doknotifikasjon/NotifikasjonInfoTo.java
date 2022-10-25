package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record NotifikasjonInfoTo(int id,
								 String bestillerId,
								 String status,
								 int antallRenotifikasjoner,
								 Set<NotifikasjonDistribusjonDto> notifikasjonDistribusjoner) {
	@Builder
	public record NotifikasjonDistribusjonDto(int id,
											  String status,
											  String kanal,
											  String kontaktInfo,
											  String tittel,
											  String tekst,
											  LocalDateTime sendtDato) {
	}
}
