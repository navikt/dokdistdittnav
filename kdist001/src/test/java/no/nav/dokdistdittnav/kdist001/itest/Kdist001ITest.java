package no.nav.dokdistdittnav.kdist001.itest;

import lombok.SneakyThrows;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist001.itest.config.ApplicationTestConfig;
import no.nav.safselvbetjening.schemas.HoveddokumentLest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles("itest")
public class Kdist001ITest extends ApplicationTestConfig {

	private static final String DOKUMENTINFO_ID = "236434";
	private static final String JOURNALPOST_ID = "153781366";
	private static final String FORSENDELSE_ID = "1720847";
	private static final String PROPERTY_JOURNALPOST = "journalpostId";

	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;
	private static final String FINNFORSENDELSE_URL = "/rest/v1/administrerforsendelse/finnforsendelse/%s/%s";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String OPPDATERDISTRIBUSJONSINFO_URL = "/rest/journalpostapi/v1/journalpost/%s/oppdaterDistribusjonsinfo";

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	@BeforeEach
	public void setUp() {
		stubAzure();
	}

	@Test
	public void skalBehandleHoveddokumentLestMelding() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentforsendelse_happy.json", OK.value());
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		putMessageOnKafkaTopic(lagHoveddokumentLest());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_JOURNALPOST, JOURNALPOST_ID))));
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(1, patchRequestedFor(urlEqualTo(format(OPPDATERDISTRIBUSJONSINFO_URL, JOURNALPOST_ID))));
			verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@Test
	public void skalAvslutteBehandlingAvHoveddokumentLestHvisForsendelseErNull() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentforsendelse_ingen_innhold.json", NO_CONTENT.value());
		stubPutOppdaterForsendelse();

		putMessageOnKafkaTopic(lagHoveddokumentLest());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_JOURNALPOST, JOURNALPOST_ID))));
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(0, patchRequestedFor(urlEqualTo(format(OPPDATERDISTRIBUSJONSINFO_URL, JOURNALPOST_ID))));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"__files/rdist001/hentforsendelse_varselstatus_ferdigstilt.json",
			"__files/rdist001/hentforsendelse_varselstatus_feilet.json"
	})
	public void skalAvslutteBehandlingAvHoveddokumentLestHvisVarselstatusErUlikOpprettet(String fil) {
		stubGetFinnForsendelse();
		stubGetHentForsendelse(fil, OK.value());
		stubPutOppdaterForsendelse();

		putMessageOnKafkaTopic(lagHoveddokumentLest());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_JOURNALPOST, JOURNALPOST_ID))));
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(0, patchRequestedFor(urlEqualTo(format(OPPDATERDISTRIBUSJONSINFO_URL, JOURNALPOST_ID))));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@Test
	public void skalAvslutteBehandlingAvHoveddokumentLestHvisHoveddokumentMangler() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentforsendelse_hoveddokument_mangler.json", OK.value());
		stubPatchOppdaterDistribusjonsinfo();

		putMessageOnKafkaTopic(lagHoveddokumentLest());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_JOURNALPOST, JOURNALPOST_ID))));
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(0, patchRequestedFor(urlEqualTo(format(OPPDATERDISTRIBUSJONSINFO_URL, JOURNALPOST_ID))));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@Test
	public void skalAvslutteBehandlingAvHoveddokumentLestHvisHoveddokumentHarFeilArkivDokumentInfoId() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentforsendelse_feil_dokumentinfoid.json", OK.value());
		stubPatchOppdaterDistribusjonsinfo();

		putMessageOnKafkaTopic(lagHoveddokumentLest());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_JOURNALPOST, JOURNALPOST_ID))));
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(0, patchRequestedFor(urlEqualTo(format(OPPDATERDISTRIBUSJONSINFO_URL, JOURNALPOST_ID))));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@Test
	public void skalAvslutteBehandlingAvHoveddokumentLestHvisDistribusjonskanalErUlikDittNav() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentforsendelse_distribusjonskanal_sdp.json", OK.value());
		stubPutOppdaterForsendelse();

		putMessageOnKafkaTopic(lagHoveddokumentLest());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_JOURNALPOST, JOURNALPOST_ID))));
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(0, patchRequestedFor(urlEqualTo(format(OPPDATERDISTRIBUSJONSINFO_URL, JOURNALPOST_ID))));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	private HoveddokumentLest lagHoveddokumentLest() {
		return HoveddokumentLest.newBuilder()
				.setDokumentInfoId(DOKUMENTINFO_ID)
				.setJournalpostId(JOURNALPOST_ID)
				.build();
	}

	private void stubGetHentForsendelse(String responsebody, int httpStatusvalue) {
		stubFor(get(urlEqualTo(HENTFORSENDELSE_URL))
				.willReturn(aResponse()
						.withStatus(httpStatusvalue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responsebody))));
	}

	void stubGetFinnForsendelse() {
		stubFor(get(FINNFORSENDELSE_URL.formatted(PROPERTY_JOURNALPOST, JOURNALPOST_ID))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rdist001/finnforsendelse_happy.json"))));
	}

	private void stubPutOppdaterForsendelse() {
		stubFor(put(OPPDATERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void putMessageOnKafkaTopic(HoveddokumentLest hoveddokumentLest) {
		kafkaEventProducer.publish(
				"teamdokumenthandtering.privat-dokdistdittnav-lestavmottaker-test", "key",
				hoveddokumentLest
		);
	}

	private void stubPatchOppdaterDistribusjonsinfo() {
		stubFor(patch(urlEqualTo(OPPDATERDISTRIBUSJONSINFO_URL.formatted(JOURNALPOST_ID)))
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	@SneakyThrows
	private static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);
	}
}
