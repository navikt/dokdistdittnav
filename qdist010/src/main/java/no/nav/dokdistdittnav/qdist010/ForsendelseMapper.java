package no.nav.dokdistdittnav.qdist010;

import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.VarseltekstfilNotFoundException;
import no.nav.dokdistdittnav.exception.technical.FinnerIkkeURLException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.ZoneId;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static no.nav.dokdistdittnav.constants.DomainConstants.BESKJED_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_AARSOPPGAVE_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VEDTAK_SMSTEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VIKTIG_SMSTEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VEDTAK_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VIKTIG_TEKST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public class ForsendelseMapper {


	public static final String BESKJED_TITTEL = "Melding fra NAV";
	public static final String AARSOPPGAVE_TITTEL = "Årsoppgave fra NAV";
	private static final String BESKJED_TEKSTFIL;
	private static final String AARSOPPPGAVE_TEKSTFIL;

	public static final String VEDTAK_EPOSTTITTEL = "Vedtak fra NAV";
	public static final String VIKTIG_EPOSTTITTEL = "Brev fra NAV";
	private static final String VEDTAK_EPOSTTEKST;
	private static final String VIKTIG_EPOSTTEKST;

	private static final String AARSOPPGAVE_DOKUMENTTYPEID = "000053";
	private static final Integer SIKKERHETSNIVAA = 3;
	private static final Integer SYNLIGEDAGER = 10;

	static {
		VEDTAK_EPOSTTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/vedtak_epostvarseltekst.html");
		VIKTIG_EPOSTTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/viktig_epostvarseltekst.html");
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

	public static BeskjedInput opprettBeskjed(String url, HentForsendelseResponse hentForsendelseResponse) {
		String dokumenttypeId = hentForsendelseResponse.getDokumenter().get(0).getDokumenttypeId();

		return new BeskjedInputBuilder()
				.withTidspunkt(now(ZoneId.of("UTC")))
				.withTekst(format(BESKJED_TEKST, hentForsendelseResponse.getForsendelseTittel()))
				.withLink(mapLink(url, hentForsendelseResponse))
				.withEksternVarsling(true)
				.withSynligFremTil(now(ZoneId.of("UTC")).plusDays(SYNLIGEDAGER))
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

	public static OppgaveInput opprettOppgave(String url, HentForsendelseResponse forsendelse) {
		var distribusjonstype = forsendelse.getDistribusjonstype();

		var oppgaveBuilder = new OppgaveInputBuilder()
				.withTidspunkt(now(ZoneId.of("UTC")))
				.withLink(mapLink(url, forsendelse))
				.withSynligFremTil(now(ZoneId.of("UTC")).plusDays(SYNLIGEDAGER))
				.withEksternVarsling(true)
				.withSikkerhetsnivaa(SIKKERHETSNIVAA);

		if (distribusjonstype == null || distribusjonstype == VIKTIG) {
			return oppgaveBuilder
					.withTekst(format(VIKTIG_TEKST, forsendelse.getForsendelseTittel()))
					.withEpostVarslingstittel(VIKTIG_EPOSTTITTEL)
					.withEpostVarslingstekst(VIKTIG_EPOSTTEKST)
					.withSmsVarslingstekst(VIKTIG_SMSTEKST)
					.build();
		}

		if (distribusjonstype == VEDTAK) {
			return oppgaveBuilder
					.withTekst(format(VEDTAK_TEKST, forsendelse.getForsendelseTittel()))
					.withEpostVarslingstittel(VEDTAK_EPOSTTITTEL)
					.withEpostVarslingstekst(VEDTAK_EPOSTTEKST)
					.withSmsVarslingstekst(VEDTAK_SMSTEKST)
					.build();
		}

		return null;
	}

	private static URL mapLink(String url, HentForsendelseResponse hentForsendelseResponse) {
		try {
			URI uri = UriComponentsBuilder
					.fromHttpUrl(url)
					.path(hentForsendelseResponse.getTema() + "/" + hentForsendelseResponse.getArkivInformasjon().getArkivId())
					.build().toUri();

			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new FinnerIkkeURLException("Path specified does not exist" + e.getMessage(), e);
		}
	}

}
