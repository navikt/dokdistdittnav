package no.nav.dokdistdittnav.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdittnav.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistdittnav.testUtils.classpathToString;
import static no.nav.dokdistdittnav.testUtils.fileToString;
import static no.nav.dokdistdittnav.testUtils.unzipToDirectory;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistdittnav.Application;
import no.nav.dokdistdittnav.itest.config.ApplicationTestConfig;
import no.nav.dokdistdittnav.storage.DokdistDokument;
import no.nav.dokdistdittnav.storage.JsonSerializer;
import no.nav.dokdistdittnav.storage.Storage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, ApplicationTestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@Disabled
public class Qdist010IT {

	private static final String FORSENDELSE_ID = "33333";
	private static final String DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK = "dokumentObjektReferanseHoveddok";
	private static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 = "dokumentObjektReferanseVedlegg1";
	private static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 = "dokumentObjektReferanseVedlegg2";
	private static final String DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK_CORRUPT = "dokumentObjektReferanseHoveddokCorrupt";
	private static final String HOVEDDOK_TEST_CONTENT = "HOVEDDOK_TEST_CONTENT";
	private static final String VEDLEGG1_TEST_CONTENT = "VEDLEGG1_TEST_CONTENT";
	private static final String VEDLEGG2_TEST_CONTENT = "VEDLEGG2_TEST_CONTENT";
	private static String CALL_ID;

	@Inject
	private JmsTemplate jmsTemplate;

	@Inject
	private Queue qdist010;

	@Inject
	private Queue qdist010FunksjonellFeil;

	@Inject
	private Queue backoutQueue;

	@Inject
	private Storage storage;

	@Inject
	public CacheManager cacheManager;

	@TempDir
	static Path tempDir;

	@BeforeAll
	public static void setupBeforeAll() throws IOException {

	}

	@AfterAll
	public static void stopServer() throws Exception {

	}

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();

		cacheManager.getCache(TKAT020_CACHE).clear();
		reset(storage);
		when(storage.get(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK)).thenReturn(Optional.of(JsonSerializer.serialize(DokdistDokument.builder()
				.pdf(HOVEDDOK_TEST_CONTENT.getBytes()).build())));
		when(storage.get(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1)).thenReturn(Optional.of(JsonSerializer.serialize(DokdistDokument.builder()
				.pdf(VEDLEGG1_TEST_CONTENT.getBytes()).build())));
		when(storage.get(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2)).thenReturn(Optional.of(JsonSerializer.serialize(DokdistDokument.builder()
				.pdf(VEDLEGG2_TEST_CONTENT.getBytes()).build())));
	}

	@Test
	public void shouldProcessForsendelse() throws Exception {

		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("regoppslag/treg002-happy.json")));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		String zippedFilePath = tempDir.toString() + "/outbound/dokdistdittnav/" + CALL_ID + ".zip";
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));

		unzipToDirectory(zippedFilePath, new File(tempDir.toString()).toPath());
		String actualBestillingXmlString = fileToString(new File(tempDir.toString() + "/" + CALL_ID + ".xml"));
		String expectedBestillingXmlString = classpathToString("/qdist010/bestilling_xml.xml").replaceAll("insertCallIdHere", CALL_ID);
		String hoveddokContent = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK + ".pdf"));
		String vedlegg1Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 + ".pdf"));
		String vedlegg2Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 + ".pdf"));

		assertEquals(expectedBestillingXmlString, actualBestillingXmlString.replaceAll("<KundeOpprettet.*KundeOpprettet>", ""));
		assertEquals(HOVEDDOK_TEST_CONTENT, hoveddokContent);
		assertEquals(VEDLEGG1_TEST_CONTENT, vedlegg1Content);
		assertEquals(VEDLEGG2_TEST_CONTENT, vedlegg2Content);

		verifyAllStubs();
	}

	@Test
	public void shouldProcessForsendelseWithoutCallingRegoppslag() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/NO")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/getPostDestinasjon-happy.json")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		String zippedFilePath = tempDir.toString() + "/outbound/dokdistdittnav/" + CALL_ID + ".zip";
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));

		unzipToDirectory(zippedFilePath, new File(tempDir.toString()).toPath());
		String actualBestillingXmlString = fileToString(new File(tempDir.toString() + "/" + CALL_ID + ".xml"));
		String expectedBestillingXmlString = classpathToString("/qdist010/bestilling_utenRegoppslag_xml.xml").replaceAll("insertCallIdHere", CALL_ID);
		String hoveddokContent = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK + ".pdf"));
		String vedlegg1Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 + ".pdf"));
		String vedlegg2Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 + ".pdf"));

		assertEquals(expectedBestillingXmlString, actualBestillingXmlString.replaceAll("<KundeOpprettet.*KundeOpprettet>", ""));
		assertEquals(HOVEDDOK_TEST_CONTENT, hoveddokContent);
		assertEquals(VEDLEGG1_TEST_CONTENT, vedlegg1Content);
		assertEquals(VEDLEGG2_TEST_CONTENT, vedlegg2Content);

		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/NO")));
	}

	@Test
	public void shouldThrowRdist001HentForsendelseFunctionalException() throws Exception {

		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}

	@Test
	public void shouldThrowRdist001HentForsendelseTechnicalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}

	@Test
	public void shouldThrowInvalidForsendelseStatusException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresseBekreftetForsendelseStatus.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}

	@Test
	public void shouldThrowTkat020FunctionalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok"))
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
	}

	@Test
	public void shouldThrowTkat020TechicalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok"))
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}

	@Test
	public void shouldThrowRegoppslagHentAdresseFunctionalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(1, postRequestedFor(urlEqualTo("/sts")));
	}

	@Test
	public void shouldThrowRegoppslagHentAdresseTechnicalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(MAX_ATTEMPTS_SHORT, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(MAX_ATTEMPTS_SHORT, postRequestedFor(urlEqualTo("/sts")));
	}

	@Test
	public void shouldThrowRdist001GetPostDestinasjonFunctionalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("regoppslag/treg002-happy.json")));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(1, postRequestedFor(urlEqualTo("/sts")));
	}

	@Test
	public void shouldThrowRdist001GetPostDestinasjonTechnicalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("regoppslag/treg002-happy.json")));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(1, postRequestedFor(urlEqualTo("/sts")));
	}

	@Test
	public void shouldThrowKunneIkkeDeserialisereS3PayloadFunctionalException() throws Exception {
		when(storage.get(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK_CORRUPT)).thenReturn(Optional.of("notJsonSerializedString"));

		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-CorruptInS3-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("regoppslag/treg002-happy.json")));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/getPostDestinasjon-happy.json")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(1, postRequestedFor(urlEqualTo("/sts")));
	}

	@Test
	public void shouldThrowDokumentIkkeFunnetIS3Exception() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddokNotInS3")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-NotInS3-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/NO")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddokNotInS3")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/NO")));
	}

	@Test
	public void shouldThrowRdist001OppdaterForsendelseStatusFunctionalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("regoppslag/treg002-happy.json")));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs();
	}


	@Test
	public void shouldThrowRdist001OppdaterForsendelseStatusTechnicalException() throws Exception {
		stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
		stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("regoppslag/treg002-happy.json")));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});
		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(MAX_ATTEMPTS_SHORT, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(1, postRequestedFor(urlEqualTo("/sts")));
	}

	private void sendStringMessage(Queue queue, final String message) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			msg.setText(message);
			msg.setStringProperty("callId", CALL_ID);
			return msg;
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}

	private void verifyAllStubs() {
		verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(1, postRequestedFor(urlEqualTo("/sts")));
	}
}



