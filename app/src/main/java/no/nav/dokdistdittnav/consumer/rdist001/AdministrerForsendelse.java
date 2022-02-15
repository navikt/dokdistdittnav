package no.nav.dokdistdittnav.consumer.rdist001;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AdministrerForsendelse {

	HentForsendelseResponseTo hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(final String forsendelseId, final String forsendelseStatus, final String varselStatus);

	HentPostDestinasjonResponseTo hentPostDestinasjon(final String landkode);
}
