package no.nav.dokdistdittnav.kdist001.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist001.itest.config.ApplicationTestConfig;
import no.nav.safselvbetjening.schemas.HoveddokumentLest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.VarselStatusCode.FERDIGSTILT;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@ActiveProfiles("itest")
public class Kdist001ITest extends ApplicationTestConfig {

	private static final String DOKUMENTINFO_ID = "236434";
	private static final String DOKUMENTINFOID_2 = "111111";
	private static final String JOURNALPOST_ID = "153781366";
	private static final String FORSENDELSE_ID = "1720847";
	private static final String URL_OPPDATERFORSENDELSE = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String URL_HENTFORSENDELSE = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	@BeforeEach
	public void stubAzureToken() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	@Test
	public void shouldReadMessageFromLestavmottakerTopicen() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPutOppdaterForsendelse(FERDIGSTILT.name(), FORSENDELSE_ID, OK.value());
		stubPatchOppdaterDistribusjonsinfo(JOURNALPOST_ID, OK.value());

		HoveddokumentLest hoveddokumentLest = HoveddokumentLest.newBuilder()
				.setDokumentInfoId(DOKUMENTINFO_ID)
				.setJournalpostId(JOURNALPOST_ID)
				.build();
		putMessageOnKafkaTopic(hoveddokumentLest);

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(URL_HENTFORSENDELSE)));
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?journalpostId=" + JOURNALPOST_ID)));
			verify(1, putRequestedFor(urlEqualTo(URL_OPPDATERFORSENDELSE)));
			verify(1, patchRequestedFor(urlMatching(".*/oppdaterDistribusjonsinfo")).withHeader("Authorization", matching("Bearer .*")));
		});
	}

	@Test
	public void shouldReadMessageFromLestavmottakerTopicenAndLogWhenDokumentInfoIdNonMatch() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-feil-dokumentinfoid.json", FORSENDELSE_ID, OK.value());
		stubPatchOppdaterDistribusjonsinfo(JOURNALPOST_ID, OK.value());

		HoveddokumentLest hoveddokumentLest = HoveddokumentLest.newBuilder()
				.setDokumentInfoId(DOKUMENTINFOID_2)
				.setJournalpostId(JOURNALPOST_ID)
				.build();
		putMessageOnKafkaTopic(hoveddokumentLest);

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?journalpostId=" + JOURNALPOST_ID)));
			verify(1, getRequestedFor(urlEqualTo(URL_HENTFORSENDELSE)));
		});
		verify(0, patchRequestedFor(urlMatching(".*/oppdaterDistribusjonsinfo")));
	}

	@Test
	public void shouldReadMessageFromLestavmottakerTopicenAndLogWhenDokdistkanalIsNotDITTNAV() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-kanal-sdp.json", FORSENDELSE_ID, OK.value());
		stubPutOppdaterForsendelse(FERDIGSTILT.name(), FORSENDELSE_ID, OK.value());

		HoveddokumentLest hoveddokumentLest = HoveddokumentLest.newBuilder()
				.setDokumentInfoId(DOKUMENTINFO_ID)
				.setJournalpostId(JOURNALPOST_ID)
				.build();

		putMessageOnKafkaTopic(hoveddokumentLest);

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?journalpostId=" + JOURNALPOST_ID)));
			verify(1, getRequestedFor(urlEqualTo(URL_HENTFORSENDELSE)));
		});
	}

	@Test
	public void hentForsendelseWithNullRekkefølgeReturnNoContent() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelse_rekkefølge_feil.json", FORSENDELSE_ID, NO_CONTENT.value());
		stubPutOppdaterForsendelse(FERDIGSTILT.name(), FORSENDELSE_ID, OK.value());

		HoveddokumentLest hoveddokumentLest = HoveddokumentLest.newBuilder()
				.setDokumentInfoId(DOKUMENTINFO_ID)
				.setJournalpostId(JOURNALPOST_ID)
				.build();
		putMessageOnKafkaTopic(hoveddokumentLest);

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?journalpostId=" + JOURNALPOST_ID)));
			verify(1, getRequestedFor(urlEqualTo(URL_HENTFORSENDELSE)));
		});
	}

	private void stubGetHentForsendelse(String responsebody, String forsendelseId, int httpStatusvalue) {
		stubFor(get(urlEqualTo("/rest/v1/administrerforsendelse/" + forsendelseId)).willReturn(aResponse().withStatus(httpStatusvalue)
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.withBody(classpathToString(responsebody))));
	}

	void stubGetFinnForsendelse(String responseBody, int httpStatusValue) {
		stubFor(WireMock.get("/administrerforsendelse/finnforsendelse?journalpostId=" + JOURNALPOST_ID)
				.willReturn(aResponse().withStatus(httpStatusValue)
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responseBody))));
	}

	private void stubPutOppdaterForsendelse(String varselStatus, String forsendelseId, int httpStatusvalue) {
		stubFor(put("/administrerforsendelse?forsendelseId=" + forsendelseId + "&varselStatus=" + varselStatus)
				.willReturn(aResponse().withStatus(httpStatusvalue)));

	}

	private void putMessageOnKafkaTopic(HoveddokumentLest hoveddokumentLest) {
		kafkaEventProducer.publish(
				"privat-dokdistdittnav-lestavmottaker", "key",
				hoveddokumentLest
		);
	}

	private void stubPatchOppdaterDistribusjonsinfo(String forsendelseId, int httpStatusvalue) {
		stubFor(patch(urlMatching("/" + forsendelseId + "/oppdaterDistribusjonsinfo"))
				.willReturn(aResponse().withStatus(httpStatusvalue)));

	}

	@SneakyThrows
	private static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);

	}
}
