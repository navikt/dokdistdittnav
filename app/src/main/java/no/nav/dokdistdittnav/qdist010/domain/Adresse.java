package no.nav.dokdistdittnav.qdist010.domain;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

@Value
@Builder
public class Adresse {

	private final String adresselinje1;
	private final String adresselinje2;
	private final String adresselinje3;
	private final String postnummer;
	private final String poststed;
	private final String landkode;
}
