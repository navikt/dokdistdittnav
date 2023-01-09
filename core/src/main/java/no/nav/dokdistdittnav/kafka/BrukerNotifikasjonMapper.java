package no.nav.dokdistdittnav.kafka;

import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.technical.FinneIkkeURLException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_AARSOPPGAVE_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VEDTAK_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VIKTIG_TEKST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;

public class BrukerNotifikasjonMapper {

	private static final String NAMESPACE = "teamdokumenthandtering";
	private static String VEDTAK_TEKST;
	private static String VIKTIG_TEKST;
	private static String BESKJED_TEKST;
	private static String AARSOPPPGAVE_TEKST;
	private static final String AARSOPPGAVE_DOKUMENTTYPEID = "000053";
	private static final String VEDTAK_TITTEL = "Vedtak fra NAV";
	private static final String VIKTIG_TITTEL = "Brev fra NAV";
	private static final String BESKJED_TITTEL = "Melding fra NAV";
	static final String AARSOPPGAVE_TITTEL = "Årsoppgave fra NAV";
	private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	private static final Integer SIKKEREHETSNIVAA = 4;

	static {
		VEDTAK_TEKST = classpathToString("__files/vedtak_epostvarseltekst.html");
		VIKTIG_TEKST = classpathToString("__files/viktig_epostvarseltekst.html");
		BESKJED_TEKST = classpathToString("__files/melding_epostvarseltekst.html");
		AARSOPPPGAVE_TEKST = classpathToString("__files/aarsoppgave_epostvarseltekst.html");
	}

	public static NokkelInput mapNokkelIntern(String forsendelseId, String appnavn, HentForsendelseResponseTo hentForsendelseResponse) {
		return new NokkelInputBuilder()
				.withEventId(hentForsendelseResponse.getBestillingsId())
				.withGrupperingsId(forsendelseId)
				.withFodselsnummer(getMottakerId(hentForsendelseResponse))
				.withNamespace(NAMESPACE)
				.withAppnavn(appnavn)
				.build();
	}

	public static NokkelInput mapNokkelForKdist002(DoneEventRequest doneEventRequest, String appnavn) {
		return new NokkelInputBuilder()
				.withEventId(doneEventRequest.getBestillingsId())
				.withGrupperingsId(doneEventRequest.getForsendelseId())
				.withFodselsnummer(doneEventRequest.getMottakerId())
				.withNamespace(NAMESPACE)
				.withAppnavn(appnavn)
				.build();
	}

	public static BeskjedInput mapBeskjedIntern(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		String dokumenttypeId = hentForsendelseResponse.getDokumenter().get(0).getDokumenttypeId();
		return new BeskjedInputBuilder()
				.withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
				.withTekst(format(BESKJED_TEKST, hentForsendelseResponse.getForsendelseTittel()))
				.withLink(mapLink(url, hentForsendelseResponse))
				.withEksternVarsling(true)
				.withEpostVarslingstekst(mapEpostTekst(dokumenttypeId))
				.withEpostVarslingstittel(mapInternEpostVarslingstittel(dokumenttypeId))
				.withSmsVarslingstekst(mapInternSmsVarslingstekst(dokumenttypeId))
				.withSikkerhetsnivaa(SIKKEREHETSNIVAA)
				.build();
	}

	private static String mapInternSmsVarslingstekst(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgpave(dokumenttypeId) ? SMS_AARSOPPGAVE_TEKST : SMS_TEKST;
	}

	private static String mapInternEpostVarslingstittel(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgpave(dokumenttypeId) ? AARSOPPGAVE_TITTEL : BESKJED_TITTEL;
	}

	private static String mapEpostTekst(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgpave(dokumenttypeId) ? AARSOPPPGAVE_TEKST : BESKJED_TEKST;
	}

	private static boolean isDokumenttypeIdAarsoppgpave(String dokumenttypeId) {
		return AARSOPPGAVE_DOKUMENTTYPEID.equals(dokumenttypeId);
	}

	public static OppgaveInput oppretteOppgave(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		return new OppgaveInputBuilder()
				.withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
				.withTekst(getTekst(hentForsendelseResponse))
				.withLink(mapLink(url, hentForsendelseResponse))
				.withEksternVarsling(true)
				.withEpostVarslingstekst(mapEpostVarslingsteks(hentForsendelseResponse.getDistribusjonstype()))
				.withEpostVarslingstittel(VEDTAK.equals(hentForsendelseResponse.getDistribusjonstype()) ? VEDTAK_TITTEL : VIKTIG_TITTEL)
				.withSmsVarslingstekst(getSmsTekst(hentForsendelseResponse))
				.withSikkerhetsnivaa(SIKKEREHETSNIVAA)
				.build();
	}

	public static DoneInput mapDoneInput() {
		return new DoneInputBuilder()
				.withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
				.build();
	}

	private static String getTekst(HentForsendelseResponseTo hentForsendelseResponse) {
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

	private static String getSmsTekst(HentForsendelseResponseTo hentForsendelseResponse) {
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

	private static String mapEpostVarslingsteks(DistribusjonsTypeKode distribusjonsType) {
		switch (distribusjonsType) {
			case VEDTAK:
				return VEDTAK_TEKST;
			case VIKTIG:
				return VIKTIG_TEKST;
			case ANNET:
				break;
		}
		return null;
	}

	private static String getMottakerId(HentForsendelseResponseTo hentForsendelseResponse) {
		HentForsendelseResponseTo.MottakerTo mottaker = hentForsendelseResponse.getMottaker();
		return ofNullable(mottaker)
				.map(HentForsendelseResponseTo.MottakerTo::getMottakerId)
				.orElseThrow(() -> new IllegalArgumentException("Mottaker kan ikke være null"));
	}

	private static URL mapLink(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		try {
			URI uri = UriComponentsBuilder.fromHttpUrl(url)
					.path(hentForsendelseResponse.getTema() + "/" + hentForsendelseResponse.getArkivInformasjon().getArkivId())
					.build().toUri();

			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new FinneIkkeURLException("Path specified does not exist" + e.getMessage(), e);
		}
	}
}
