package no.nav.dokdistdittnav.kafka;

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;

import java.time.ZoneId;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.requireNonNull;
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

	public NokkelInput mapNokkelIntern(String forsendelseId, HentForsendelseResponseTo hentForsendelseResponse) {
		return NokkelInput.newBuilder()
				.setEventId(hentForsendelseResponse.getBestillingsId())
				.setGrupperingsId(forsendelseId)
				.setFodselsnummer(getMottakerId(hentForsendelseResponse))
				.setNamespace(NAMESPACE)
				.setAppnavn(APP_NAVN)
				.build();
	}

	public BeskjedInput mapBeskjedIntern(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		return BeskjedInput.newBuilder()
				.setTidspunkt(now(ZoneId.of("UTC")).toEpochSecond())
				.setTekst(format(BESKJED_TEKST, hentForsendelseResponse.getForsendelseTittel()))
				.setLink(mapLink(url, hentForsendelseResponse))
				.setEksternVarsling(true)
				.setEpostVarslingstekst(classpathToString(BESKJED_PATH))
				.setEpostVarslingstittel(BESKJED_TITTEL)
				.setSmsVarslingstekst(SMS_TEKST)
				.build();
	}

	public OppgaveInput oppretteOppgave(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		return OppgaveInput.newBuilder()
				.setTidspunkt(now(ZoneId.of("UTC")).toEpochSecond())
				.setTekst(getTekst(hentForsendelseResponse))
				.setLink(mapLink(url, hentForsendelseResponse))
				.setEksternVarsling(true)
				.setEpostVarslingstekst(mapEpostVarslingsteks(hentForsendelseResponse.getDistribusjonstype()))
				.setEpostVarslingstittel(VEDTAK.equals(hentForsendelseResponse.getDistribusjonstype()) ? VEDTAK_TITTEL : VIKTIG_TITTEL)
				.setSmsVarslingstekst(getSmsTekst(hentForsendelseResponse))
				.build();
	}

	public DoneInput mapDoneInput() {
		return DoneInput.newBuilder()
				.setTidspunkt(now(ZoneId.of("UTC")).toEpochSecond())
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

	private String mapLink(String url, HentForsendelseResponseTo hentForsendelseResponse) {
		return format(url + "%s#%s", hentForsendelseResponse.getTema(),
				requireNonNull(hentForsendelseResponse.getArkivInformasjon().getArkivId(), "jornalpostId kan ikke være null"));
	}
}
