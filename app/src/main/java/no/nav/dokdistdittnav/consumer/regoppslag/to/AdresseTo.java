package no.nav.dokdistdittnav.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */

@Value
@Builder
public class AdresseTo {

	private final String adresselinje1;
	private final String adresselinje2;
	private final String adresselinje3;
	private final String postnummer;
	private final String poststed;
	private final String landkode;
}
