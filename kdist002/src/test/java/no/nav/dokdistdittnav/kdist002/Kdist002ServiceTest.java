package no.nav.dokdistdittnav.kdist002;

import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.doknotifikasjon.DoknotifikasjonConsumer;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseRequestTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseRequest;
import no.nav.dokdistdittnav.consumer.rdist001.to.OpprettForsendelseResponse;
import no.nav.dokdistdittnav.kafka.DoneEventRequest;
import no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static no.nav.dokdistdittnav.kdist002.TestUtils.hentForsendelseResponse;
import static no.nav.dokdistdittnav.kdist002.TestUtils.hentForsendelseResponseUtenMottaker;
import static no.nav.dokdistdittnav.kdist002.TestUtils.hentForsendelseResponseWithForsendelseStatusFeilet;
import static no.nav.dokdistdittnav.kdist002.TestUtils.hentNotifikasjonInfoTo;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.INFO;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.OVERSENDT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Kdist002ServiceTest {

	private AdministrerForsendelse administrerForsendelse;
	private DoknotifikasjonConsumer doknotifikasjonConsumer;

	@Autowired
	private Kdist002Service kdist002Service;

	@BeforeEach
	public void setUp() {
		administrerForsendelse = mock(AdministrerForsendelseConsumer.class);
		doknotifikasjonConsumer = mock(DoknotifikasjonConsumer.class);
		kdist002Service = new Kdist002Service(dokdistdittnavProperties(), administrerForsendelse, doknotifikasjonConsumer);
	}

	@ParameterizedTest
	@EnumSource(value = DoknotifikasjonStatusKode.class, names = {"OVERSENDT", "FERDIGSTILT"})
	public void shouldUpdateDistribusjonsTidspunktAndForsendelseStatus(DoknotifikasjonStatusKode statusKode) {
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(doknotifikasjonConsumer.getNotifikasjonInfo(DOKNOTIFIKASJON_BESTILLINGSID_OLD)).thenReturn(hentNotifikasjonInfoTo());
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponse());

		DoneEventRequest doneEventRequest = assertDoesNotThrow(() -> kdist002Service.sendForsendelse(doknotifikasjonStatusWithoutDistribusjonsId(DOKDISTDITTNAV, statusKode.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		verify(administrerForsendelse, times(1)).finnForsendelse(any());
		verify(administrerForsendelse, times(1)).oppdaterVarselInfo(any());
		verify(administrerForsendelse, times(1)).oppdaterForsendelseStatus(any(), anyString());
		assertNull(doneEventRequest);
	}
	@Test
	public void shouldNotUpdateForsendelseStatusWhenStatusIsFEILET() {
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(doknotifikasjonConsumer.getNotifikasjonInfo(DOKNOTIFIKASJON_BESTILLINGSID_OLD)).thenReturn(hentNotifikasjonInfoTo());
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponseWithForsendelseStatusFeilet());

		DoneEventRequest doneEventRequest = assertDoesNotThrow(() -> kdist002Service.sendForsendelse(doknotifikasjonStatusWithoutDistribusjonsId(DOKDISTDITTNAV, OVERSENDT.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		verify(administrerForsendelse, times(1)).finnForsendelse(any());
		verify(administrerForsendelse, times(1)).oppdaterVarselInfo(any());
		verify(administrerForsendelse, times(0)).oppdaterForsendelseStatus(any(), anyString());
		assertNull(doneEventRequest);
	}
	@Test
	public void shouldLogAndAvsluttBehandlingenWhenForsendelseStatusErNotFeilet() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponse());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = assertDoesNotThrow(() -> kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertNotNull(doneEventRequest);
	}

	@Test
	public void shouldExtractOldBestillingsIdFraDoknotifikasjonStatusEvent() {
		when(administrerForsendelse.hentForsendelse(FORSENDELSE_ID)).thenReturn(hentForsendelseResponse());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD));

		assertNotNull(doneEventRequest);
		assertEquals(hentForsendelseResponse().getMottaker().getMottakerId(), doneEventRequest.getMottakerId());
	}

	@Test
	public void shouldExtractNewBestillingsIdFraDoknotifikasjonStatusEvent() {
		when(administrerForsendelse.hentForsendelse(FORSENDELSE_ID)).thenReturn(hentForsendelseResponse());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_NEW));

		assertNotNull(doneEventRequest);
		assertEquals(hentForsendelseResponse().getMottaker().getMottakerId(), doneEventRequest.getMottakerId());
	}

	@Test
	public void shouldLogAndAvsluttBehandlingenWhenForsendelseStatusErNotFeiletAndBestillerIdIsNotDokdistdittnav() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponse());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = assertDoesNotThrow(() -> kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDPI, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertNotNull(doneEventRequest);
	}

	@Test
	public void shouldLogAndAvsluttBehandlingenWhenNotifikasjonStatusErNotFeiletAndBestillerIdIsNotDokdistdittnav() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponse());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = assertDoesNotThrow(() -> kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDPI, INFO.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertNull(doneEventRequest);
	}

	@Test
	public void shouldLogAndAvsluttBehandlingenWhenForsendelseStatusErFEILET() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponseWithForsendelseStatusFeilet());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		DoneEventRequest doneEventRequest = kdist002Service.sendForsendelse(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD));

		assertNull(doneEventRequest);
	}

	@Test
	public void shouldThrowExceptionIfFinnForsendelseReturnsNull() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponse());
		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(null);
		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
				.forsendelseId(Long.valueOf(FORSENDELSE_ID))
				.build());

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> kdist002Service.sendForsendelse(
				doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name(), DOKNOTIFIKASJON_BESTILLINGSID_OLD)));

		assertEquals("finnForsendelseResponseTo kan ikke være null", e.getMessage());
	}

	@Test
	public void throwsExceptionWhenHentForsendleseWithoutMottakerInfo() {
		HentForsendelseResponse hentForsendelseResponse = hentForsendelseResponseUtenMottaker();

		when(administrerForsendelse.finnForsendelse(FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(PROPERTY_BESTILLINGS_ID)
				.verdi(BESTILLINGSID)
				.build())).thenReturn(finnForsendelseResponseTo());
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(hentForsendelseResponse);

		when(administrerForsendelse.opprettForsendelse(any(OpprettForsendelseRequest.class))).thenReturn(OpprettForsendelseResponse.builder()
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

	public DoknotifikasjonStatus doknotifikasjonStatusWithoutDistribusjonsId(String appnavn, String status, String bestillingsId) {
		return DoknotifikasjonStatus.newBuilder()
				.setBestillerId(appnavn)
				.setBestillingsId(bestillingsId)
				.setStatus(status)
				.setMelding(MELDING)
				.build();
	}

	public DokdistdittnavProperties dokdistdittnavProperties() {
		DokdistdittnavProperties dokdistdittnavProperties = new DokdistdittnavProperties();
		dokdistdittnavProperties.setAppnavn(DOKDISTDITTNAV);
		return dokdistdittnavProperties;
	}

}