package no.nav.dokdistdittnav.consumer.dokumentdistribusjon;

public record Notifikasjon(
		String kanal,
		String tittel,
		String tekst,
		String kontaktInfo
) {
}
