package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
@AllArgsConstructor
public class NotifikasjonDistribusjonDto {
	private final int id;
	private final String status;
	private final String kanal;
	private final String kontaktInfo;
	private final String tittel;
	private final String tekst;
	private final LocalDateTime sendtDato;
}
