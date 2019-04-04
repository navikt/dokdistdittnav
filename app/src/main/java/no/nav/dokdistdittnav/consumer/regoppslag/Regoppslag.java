package no.nav.dokdistdittnav.consumer.regoppslag;

import no.nav.dokdistdittnav.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistdittnav.consumer.regoppslag.to.HentAdresseRequestTo;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface Regoppslag {

	AdresseTo treg002HentAdresse(HentAdresseRequestTo request);
}