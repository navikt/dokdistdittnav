package no.nav.dokdistdittnav.consumer.dokumentdistribusjon;

import java.time.LocalDateTime;

public record Notifikasjon(
		String kanal,
		String tittel,
		String tekst,
		String kontaktInfo,
		LocalDateTime varslingstidspunkt
) {
}
