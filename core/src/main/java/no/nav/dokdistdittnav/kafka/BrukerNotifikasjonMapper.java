package no.nav.dokdistdittnav.kafka;

import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse.MottakerTo;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;

public class BrukerNotifikasjonMapper {

	private static final String NAMESPACE = "teamdokumenthandtering";

	public NokkelInput mapNokkelIntern(String forsendelseId, String appnavn, HentForsendelseResponse forsendelse) {
		return new NokkelInputBuilder()
				.withEventId(forsendelse.getBestillingsId())
				.withGrupperingsId(forsendelseId)
				.withFodselsnummer(getMottakerId(forsendelse))
				.withNamespace(NAMESPACE)
				.withAppnavn(appnavn)
				.build();
	}

	public DoneInput mapDoneInput() {
		return new DoneInputBuilder()
				.withTidspunkt(now(UTC))
				.build();
	}

	private String getMottakerId(HentForsendelseResponse forsendelse) {
		MottakerTo mottaker = forsendelse.getMottaker();

		if (mottaker == null || mottaker.getMottakerId() == null) {
			throw new IllegalArgumentException("Mottaker kan ikke være null");
		}

		return mottaker.getMottakerId();
	}

}
