package no.nav.dokdistdittnav.qdist010;

import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.VarseltekstfilNotFoundException;
import no.nav.dokdistdittnav.exception.technical.FinnerIkkeURLException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.constants.DomainConstants.BESKJED_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_AARSOPPGAVE_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VEDTAK_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VIKTIG_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VEDTAK_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VIKTIG_TEKST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public class ForsendelseMapper {

	private static final String VEDTAK_TEKSTFIL;
	private static final String VIKTIG_TEKSTFIL;
	private static final String BESKJED_TEKSTFIL;
	private static final String AARSOPPPGAVE_TEKSTFIL;
	private static final String AARSOPPGAVE_DOKUMENTTYPEID = "000053";
	public static final String VEDTAK_TITTEL = "Vedtak fra NAV";
	public static final String VIKTIG_TITTEL = "Brev fra NAV";
	public static final String BESKJED_TITTEL = "Melding fra NAV";
	public static final String AARSOPPGAVE_TITTEL = "Årsoppgave fra NAV";
	private static final Integer SIKKERHETSNIVAA = 3;

	static {
		VEDTAK_TEKSTFIL = getFileAndAssertNotNullOrEmpty("varseltekster/vedtak_epostvarseltekst.html");
		VIKTIG_TEKSTFIL = getFileAndAssertNotNullOrEmpty("varseltekster/viktig_epostvarseltekst.html");
		BESKJED_TEKSTFIL = getFileAndAssertNotNullOrEmpty("varseltekster/melding_epostvarseltekst.html");
		AARSOPPPGAVE_TEKSTFIL = getFileAndAssertNotNullOrEmpty("varseltekster/aarsoppgave_epostvarseltekst.html");
	}

	private static String getFileAndAssertNotNullOrEmpty(String path) {
		String result = classpathToString(path);
		if (isEmpty(result)) {
			throw new VarseltekstfilNotFoundException("Fant ikke filen på path: " + path);
		}
		return result;
	}

	public static BeskjedInput mapBeskjedIntern(String url, HentForsendelseResponse hentForsendelseResponse) {
		String dokumenttypeId = hentForsendelseResponse.getDokumenter().get(0).getDokumenttypeId();

		return new BeskjedInputBuilder()
				.withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
				.withTekst(format(BESKJED_TEKST, hentForsendelseResponse.getForsendelseTittel()))
				.withLink(mapLink(url, hentForsendelseResponse))
				.withEksternVarsling(true)
				.withEpostVarslingstekst(mapEpostTekst(dokumenttypeId))
				.withEpostVarslingstittel(mapInternEpostVarslingstittel(dokumenttypeId))
				.withSmsVarslingstekst(mapInternSmsVarslingstekst(dokumenttypeId))
				.withSikkerhetsnivaa(SIKKERHETSNIVAA)
				.build();
	}

	private static String mapInternSmsVarslingstekst(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgave(dokumenttypeId) ? SMS_AARSOPPGAVE_TEKST : SMS_TEKST;
	}

	private static String mapInternEpostVarslingstittel(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgave(dokumenttypeId) ? AARSOPPGAVE_TITTEL : BESKJED_TITTEL;
	}

	private static String mapEpostTekst(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgave(dokumenttypeId) ? AARSOPPPGAVE_TEKSTFIL : BESKJED_TEKSTFIL;
	}

	private static boolean isDokumenttypeIdAarsoppgave(String dokumenttypeId) {
		return AARSOPPGAVE_DOKUMENTTYPEID.equals(dokumenttypeId);
	}

	public static OppgaveInput opprettOppgave(String url, HentForsendelseResponse hentForsendelseResponse) {
		return new OppgaveInputBuilder()
				.withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
				.withTekst(getTekst(hentForsendelseResponse))
				.withLink(mapLink(url, hentForsendelseResponse))
				.withEksternVarsling(true)
				.withEpostVarslingstekst(mapEpostVarslingstekst(hentForsendelseResponse.getDistribusjonstype()))
				.withEpostVarslingstittel(VEDTAK.equals(hentForsendelseResponse.getDistribusjonstype()) ? VEDTAK_TITTEL : VIKTIG_TITTEL)
				.withSmsVarslingstekst(getSmsTekst(hentForsendelseResponse))
				.withSikkerhetsnivaa(SIKKERHETSNIVAA)
				.build();
	}


	private static String getTekst(HentForsendelseResponse hentForsendelseResponse) {
		switch (hentForsendelseResponse.getDistribusjonstype()) {
			case VEDTAK:
				return format(VEDTAK_TEKST, hentForsendelseResponse.getForsendelseTittel());
			case VIKTIG:
				return format(VIKTIG_TEKST, hentForsendelseResponse.getForsendelseTittel());
			case ANNET:
				break;
		}
		return null;
	}

	private static String getSmsTekst(HentForsendelseResponse hentForsendelseResponse) {
		switch (hentForsendelseResponse.getDistribusjonstype()) {
			case VEDTAK:
				return SMS_VEDTAK_TEKST;
			case VIKTIG:
				return SMS_VIKTIG_TEKST;
			case ANNET:
				break;
		}
		return null;
	}

	private static String mapEpostVarslingstekst(DistribusjonsTypeKode distribusjonsType) {
		switch (distribusjonsType) {
			case VEDTAK:
				return VEDTAK_TEKSTFIL;
			case VIKTIG:
				return VIKTIG_TEKSTFIL;
			case ANNET:
				break;
		}
		return null;
	}

	private static URL mapLink(String url, HentForsendelseResponse hentForsendelseResponse) {
		try {
			URI uri = UriComponentsBuilder.fromHttpUrl(url)
					.path(hentForsendelseResponse.getTema() + "/" + hentForsendelseResponse.getArkivInformasjon().getArkivId())
					.build().toUri();

			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new FinnerIkkeURLException("Path specified does not exist" + e.getMessage(), e);
		}
	}
}
