package no.nav.dokdistdittnav.kdist002;

import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.PersisterForsendelseResponseTo;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.kdist002.TestUtils.BESTILLINGSID;
import static no.nav.dokdistdittnav.kdist002.TestUtils.DOKDISTDITTNAV;
import static no.nav.dokdistdittnav.kdist002.TestUtils.DOKDISTDPI;
import static no.nav.dokdistdittnav.kdist002.TestUtils.DOKNOTIFIKASJON_BESTILLINGSID_NEW;
import static no.nav.dokdistdittnav.kdist002.TestUtils.DOKNOTIFIKASJON_BESTILLINGSID_OLD;
import static no.nav.dokdistdittnav.kdist002.TestUtils.FORSENDELSE_ID;
import static no.nav.dokdistdittnav.kdist002.TestUtils.MELDING;
import static no.nav.dokdistdittnav.kdist002.TestUtils.finnForsendelseResponseTo;
import static no.nav.dokdistdittnav.kdist002.TestUtils.hentForsendelseResponseTo;
import static no.nav.dokdistdittnav.kdist002.TestUtils.hentForsendelseResponseWithForsendelseStatusFeilet;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.INFO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Kdist002ServiceTest {

	private DokdistdittnavProperties dokdistdittnavProperties;
	private AdministrerForsendelse administrerForsendelse;

	@Autowired
	private Kdist002Service kdist002Service;

	@BeforeEach
	public void setUp() {
		administrerForsendelse = mock(AdministrerForsendelseConsumer.class);
		dokdistdittnavProperties = dokdistdittnavProperties();
		kdist002Service = new Kdist002Service(dokdistdittnavProperties, administrerForsendelse);
	}

	@Test
	public void shouldExtractOldBestillingsIdFraDoknotifikasjonStatusEvent() {
		when(administrerForsendelse.hentForsendelse(FORSENDELSE_ID)).thenReturn(hentForsendelseResponseTo());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.persisterForsendelse(any(PersisterForsendelseRequestTo.class))).thenReturn(PersisterForsendelseResponseTo.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD));

		assertNotNull(doneEventRequest);
		assertEquals(hentForsendelseResponseTo().getMottaker().getMottakerId(), doneEventRequest.getMottakerId());
	}

	@Test
	public void shouldExtractNewBestillingsIdFraDoknotifikasjonStatusEvent() {
		when(administrerForsendelse.hentForsendelse(FORSENDELSE_ID)).thenReturn(hentForsendelseResponseTo());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.persisterForsendelse(any(PersisterForsendelseRequestTo.class))).thenReturn(PersisterForsendelseResponseTo.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_NEW));

		assertNotNull(doneEventRequest);
		assertEquals(hentForsendelseResponseTo().getMottaker().getMottakerId(), doneEventRequest.getMottakerId());
	}

	@Test
	public void shouldLogAndAvsluttBehandlingenWhenNotifikasjonStatusErNotFeilet() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponseTo());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.persisterForsendelse(any(PersisterForsendelseRequestTo.class))).thenReturn(PersisterForsendelseResponseTo.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = assertDoesNotThrow(() -> kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, INFO.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertNotNull(doneEventRequest);
	}

	@Test
	public void shouldLogAndAvsluttBehandlingenWhenNotifikasjonStatusErNotFeiletAndBestillerIdIsNotDokdistdittnav() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponseTo());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.persisterForsendelse(any(PersisterForsendelseRequestTo.class))).thenReturn(PersisterForsendelseResponseTo.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = assertDoesNotThrow(() -> kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDPI, INFO.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertNotNull(doneEventRequest);
	}

	@Test
	public void shouldLogAndAvsluttBehandlingenWhenForsendelseStatusErFEILET() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponseWithForsendelseStatusFeilet());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.persisterForsendelse(any(PersisterForsendelseRequestTo.class))).thenReturn(PersisterForsendelseResponseTo.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD));

		assertNull(doneEventRequest);
	}

	@Test
	public void shouldThrowExceptionIfFinnForsendelseReturnsNull() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponseTo());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(null);
		when(administrerForsendelse.persisterForsendelse(any(PersisterForsendelseRequestTo.class))).thenReturn(PersisterForsendelseResponseTo.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> kdist002Service.sendForsendelse(
				doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertEquals("finnForsendelseResponseTo kan ikke være null", e.getMessage());
	}

	@Test
	public void throwsExceptionWhenHentForsendleseWithoutMottakerInfo() {
		HentForsendelseResponseTo hentForsendelseResponseTo = hentForsendelseResponseTo();
		hentForsendelseResponseTo.setMottaker(null);
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponseTo);

		when(administrerForsendelse.persisterForsendelse(any(PersisterForsendelseRequestTo.class))).thenReturn(PersisterForsendelseResponseTo.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> kdist002Service.sendForsendelse(
				doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertEquals("Mottaker kan ikke være null", e.getMessage());
	}

	public DoknotifikasjonStatus doknotifikasjonStatus(String appnavn, String status, String bestillingsId) {
		return DoknotifikasjonStatus.newBuilder()
				.setBestillerId(appnavn)
				.setBestillingsId(bestillingsId)
				.setStatus(status)
				.setDistribusjonId(1L)
				.setMelding(MELDING)
				.build();
	}

	public DokdistdittnavProperties dokdistdittnavProperties() {
		DokdistdittnavProperties dokdistdittnavProperties = new DokdistdittnavProperties();
		dokdistdittnavProperties.setAppnavn(DOKDISTDITTNAV);
		return dokdistdittnavProperties;
	}

}