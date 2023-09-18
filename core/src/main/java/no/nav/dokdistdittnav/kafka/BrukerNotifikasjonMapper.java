package no.nav.dokdistdittnav.kafka;

import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.MottakerTo;

import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;
import static java.util.Optional.ofNullable;

public class BrukerNotifikasjonMapper {

	private static final String NAMESPACE = "teamdokumenthandtering";

	public NokkelInput mapNokkelIntern(String forsendelseId, String appnavn, HentForsendelseResponse hentForsendelseResponse) {
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
				.withTidspunkt(LocalDateTime.now(UTC))
				.build();
	}

	private String getMottakerId(HentForsendelseResponse hentForsendelseResponse) {
		MottakerTo mottaker = hentForsendelseResponse.getMottaker();

		return ofNullable(mottaker)
				.map(MottakerTo::getMottakerId)
				.orElseThrow(() -> new IllegalArgumentException("Mottaker kan ikke være null"));
	}

}
