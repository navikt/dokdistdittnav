package no.nav.dokdistdittnav.kafka;

import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.util.Optional.ofNullable;

public class BrukerNotifikasjonMapper {

	private static final String NAMESPACE = "teamdokumenthandtering";

	public NokkelInput mapNokkelIntern(String forsendelseId, String appnavn, HentForsendelseResponseTo hentForsendelseResponse) {
		return new NokkelInputBuilder()
				.withEventId(hentForsendelseResponse.getBestillingsId())
				.withGrupperingsId(forsendelseId)
				.withFodselsnummer(getMottakerId(hentForsendelseResponse))
				.withNamespace(NAMESPACE)
				.withAppnavn(appnavn)
				.build();
	}

	public NokkelInput mapNokkelForKdist002(DoneEventRequest doneEventRequest, String appnavn) {
		return new NokkelInputBuilder()
				.withEventId(doneEventRequest.getDittnavBestillingsId())
				.withGrupperingsId(doneEventRequest.getDittnavFeiletForsendelseId())
				.withFodselsnummer(doneEventRequest.getMottakerId())
				.withNamespace(NAMESPACE)
				.withAppnavn(appnavn)
				.build();
	}


	public DoneInput mapDoneInput() {
		return new DoneInputBuilder()
				.withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
				.build();
	}

	private String getMottakerId(HentForsendelseResponseTo hentForsendelseResponse) {
		HentForsendelseResponseTo.MottakerTo mottaker = hentForsendelseResponse.getMottaker();
		return ofNullable(mottaker)
				.map(HentForsendelseResponseTo.MottakerTo::getMottakerId)
				.orElseThrow(() -> new IllegalArgumentException("Mottaker kan ikke være null"));
	}

}
