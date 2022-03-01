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
import java.util.TimeZone;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static no.nav.dokdistdittnav.constants.DomainConstants.BESKJED_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VEDTAK_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.SMS_VIKTIG_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VEDTAK_TEKST;
import static no.nav.dokdistdittnav.constants.DomainConstants.VIKTIG_TEKST;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.kafka.FunctionalUtils.classpathToString;

public class BrukerNotifikasjonMapper {

	private static final String NAMESPACE = "teamdokumenthandtering";
	private static final String APP_NAVN = "dokdistdittnav";
	private static final String VEDTAK_PATH = "__files/vedtak_epostvarseltekst.html";
	private static final String VIKTIG_PATH = "__files/viktig_epostvarseltekst.html";
	private static final String BESKJED_PATH = "__files/melding_epostvarseltekst.html";
	private static final String VEDTAK_TITTEL = "Vedtak fra NAV";
	private static final String VIKTIG_TITTEL = "Brev fra NAV";
	private static final String BESKJED_TITTEL = "Melding fra NAV";
	private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	private static final Integer SIKKEREHETSNIVAA = 4;

	public NokkelInput mapNokkelIntern(String forsendelseId, HentForsendelseResponseTo hentForsendelseResponse) {
		return new NokkelInputBuilder()
				.withEventId(hentForsendelseResponse.getBestillingsId())
				.withGrupperingsId(forsendelseId)
				.withFodselsnummer(getMottakerId(hentForsendelseResponse))
				.withNamespace(NAMESPACE)
				.withAppnavn(APP_NAVN)
				.build();
	}

	public BeskjedInput mapBeskjedIntern(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		return new BeskjedInputBuilder()
				.withTidspunkt(LocalDateTime.now(DEFAULT_TIME_ZONE.toZoneId()))
				.withTekst(format(BESKJED_TEKST, hentForsendelseResponse.getForsendelseTittel()))
				.withLink(mapLink(url, hentForsendelseResponse))
				.withEksternVarsling(true)
				.withEpostVarslingstekst(classpathToString(BESKJED_PATH))
				.withEpostVarslingstittel(BESKJED_TITTEL)
				.withSmsVarslingstekst(SMS_TEKST)
				.withSikkerhetsnivaa(SIKKEREHETSNIVAA)
				.build();
	}

	public OppgaveInput oppretteOppgave(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		return new OppgaveInputBuilder()
				.withTidspunkt(LocalDateTime.now(DEFAULT_TIME_ZONE.toZoneId()))
				.withTekst(getTekst(hentForsendelseResponse))
				.withLink(mapLink(url, hentForsendelseResponse))
				.withEksternVarsling(true)
				.withEpostVarslingstekst(mapEpostVarslingsteks(hentForsendelseResponse.getDistribusjonstype()))
				.withEpostVarslingstittel(VEDTAK.equals(hentForsendelseResponse.getDistribusjonstype()) ? VEDTAK_TITTEL : VIKTIG_TITTEL)
				.withSmsVarslingstekst(getSmsTekst(hentForsendelseResponse))
				.withSikkerhetsnivaa(SIKKEREHETSNIVAA)
				.build();
	}

	public DoneInput mapDoneInput() {
		return new DoneInputBuilder()
				.withTidspunkt(LocalDateTime.now(DEFAULT_TIME_ZONE.toZoneId()))
				.build();
	}

	private String getTekst(HentForsendelseResponseTo hentForsendelseResponse) {
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

	private String getSmsTekst(HentForsendelseResponseTo hentForsendelseResponse) {
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

	private String mapEpostVarslingsteks(DistribusjonsTypeKode distribusjonsType) {
		switch (distribusjonsType) {
			case VEDTAK:
				return classpathToString(VEDTAK_PATH);
			case VIKTIG:
				return classpathToString(VIKTIG_PATH);
			case ANNET:
				break;
		}
		return null;
	}

	private String getMottakerId(HentForsendelseResponseTo hentForsendelseResponse) {
		HentForsendelseResponseTo.MottakerTo mottaker = hentForsendelseResponse.getMottaker();
		return ofNullable(mottaker)
				.map(HentForsendelseResponseTo.MottakerTo::getMottakerId)
				.orElseThrow(() -> new IllegalArgumentException("Mottaker kan ikke være null"));
	}

	private URL mapLink(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		try {
			URI uri = UriComponentsBuilder.fromHttpUrl(url)
					.path(hentForsendelseResponse.getTema() + "#" + hentForsendelseResponse.getArkivInformasjon().getArkivId())
					.build().toUri();

			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new FinneIkkeURLException("Path specified does not exist" + e.getMessage(), e);
		}
	}
}
