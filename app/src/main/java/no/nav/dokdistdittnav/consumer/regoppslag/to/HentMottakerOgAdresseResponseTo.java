package no.nav.dokdistdittnav.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentMottakerOgAdresseResponseTo {

	private final AdresseTo adresse;
}
