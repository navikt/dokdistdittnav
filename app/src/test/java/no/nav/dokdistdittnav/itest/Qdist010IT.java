package no.nav.dokdistdittnav.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdittnav.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdittnav.config.cache.LokalCacheConfig.TKAT021_CACHE;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistdittnav.testUtils.classpathToString;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistdittnav.Application;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.InputSource;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist010IT {

	private static final String FORSENDELSE_ID = "33333";
	private static final String DOKUMENTTYPE_ID_HOVEDDOK = "dokumenttypeIdHoveddok";
	private static final String VARSEL_TYPE_ID = "DittNAV_000004";
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
	private Queue dokumentHenvendelse;

	@Inject
	private Queue varselUtsending;

	@Inject
	public CacheManager cacheManager;

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();

		cacheManager.getCache(TKAT020_CACHE).clear();
		cacheManager.getCache(TKAT021_CACHE).clear();
	}

	@Test
	public void shouldProcessForsendelse() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get(urlMatching("/varselinfo/" + VARSEL_TYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("varselinfov1/tkat021-happy.json")));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(dokumentHenvendelse);
			assertThat(henvendelseReplace(response),
					is(henvendelseReplace(classpathToString("opprettDokumenthenvendelse/opprettDokumenthenvendelse-happy.xml"))));
			XPath xPath = XPathFactory.newInstance().newXPath();
			String varselbestillingIdHenvendelse = xPath.evaluate("//varselbestillingId/text()", new InputSource( new StringReader(response)));
			assertThat(varselbestillingIdHenvendelse, not(isEmptyOrNullString()));
			try {
				UUID.fromString(varselbestillingIdHenvendelse);
			} catch (IllegalArgumentException e) {
				fail("varselbestillingId is not a UUID");
			}
			String ferdigstiltDatoHenvendelse = xPath.evaluate("//ferdigstiltDato/text()", new InputSource( new StringReader(response)));

			response = receive(varselUtsending);
			assertThat(varselReplace(response),
					is(varselReplace(classpathToString("tvarsel003/tvarsel003-happy.xml"))));
			String varselbestillingIdVarsel = xPath.evaluate("//parameterListe[key/text()='VarselbestillingsId']/value/text()",
					new InputSource( new StringReader(response)));
			assertThat(varselbestillingIdVarsel, is(varselbestillingIdHenvendelse));
			String ferdigstiltDatoVarsel = xPath.evaluate("//parameterListe[key/text()='FerdigstiltDato']/value/text()",
					new InputSource( new StringReader(response)));
			assertThat(ferdigstiltDatoVarsel, is(ferdigstiltDatoHenvendelse));
		});

		verifyAllStubs(1);
	}

	@Test
	public void shouldProcessForsendelseRepeterendeVarsel() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get(urlMatching("/varselinfo/" + VARSEL_TYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("varselinfov1/tkat021-utenRevarslingIntervall.json")));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(dokumentHenvendelse);
			assertThat(henvendelseReplace(response),
					is(henvendelseReplace(classpathToString("opprettDokumenthenvendelse/opprettDokumenthenvendelse-repeterendeVarsel-happy.xml"))));

			response = receive(varselUtsending);
			assertThat(varselReplace(response),
					is(varselReplace(classpathToString("tvarsel003/tvarsel003-happy.xml"))));
		});

		verifyAllStubs(1);
	}

	@Test
	public void shouldThrowForsendelseManglerPaakrevdHeaderFunctionalExceptionManglerCallId() throws Exception {

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"), null);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	public void shouldThrowForsendelseManglerPaakrevdHeaderFunctionalExceptionEmptyCallId() throws Exception {

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"), "");

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	public void shouldThrowForsendelseManglerForsendelseIdFunctionalExceptionManglerForsendelseId() throws Exception {

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-feilId.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-feilId.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	public void shouldThrowForsendelseManglerForsendelseIdFunctionalExceptionTomForsendelseId() throws Exception {

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-tom-forsendelseId.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-tom-forsendelseId.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	public void shouldThrowRdist001HentForsendelseFunctionalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil, CALL_ID);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(0, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowRdist001HentForsendelseTechnicalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue, CALL_ID);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(0, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowRdist001HentForsendelseFunctionalExceptionUtenArkivInformasjon() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_utenArkivInformasjon.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(0, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowInvalidForsendelseStatusException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_oversendtForsendelseStatus.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(0, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowTkat020FunctionalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK))
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowTkat020FunctionalExceptionUtenDokumentProduksjonsInfo() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-utenDokumentProduksjonsInfo.json")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowTkat020FunctionalExceptionUtenDistribusjonInfo() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-utenDistribusjonInfo.json")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowTkat020FunctionalExceptionUtenDittNavDistribusjonVarsel() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-utenDittNavDistribusjonVarsel.json")));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowTkat020TechnicalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK))
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(0, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowTkat021FunctionalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get(urlMatching("/varselinfo/" + VARSEL_TYPE_ID))
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(1, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowTkat021TechnicalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get(urlMatching("/varselinfo/" + VARSEL_TYPE_ID))
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	public void shouldThrowRdist001OppdaterForsendelseStatusFunctionalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get(urlMatching("/varselinfo/" + VARSEL_TYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("varselinfov1/tkat021-happy.json")));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));

			// funksjonelle feil ruller ikke tilbake route, så siden meldinger her allerede er lagt på kø,
			// motta meldingene for å unngå problemer med andre tester. Det er lite sannsynlig at dette
			// feilscenarioet vil forekomme.
			receive(dokumentHenvendelse);
			receive(varselUtsending);
		});

		verifyAllStubs(1);
	}

	@Test
	public void shouldThrowRdist001OppdaterForsendelseStatusTechnicalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get(urlMatching("/varselinfo/" + VARSEL_TYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("varselinfov1/tkat021-happy.json")));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(1, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(MAX_ATTEMPTS_SHORT, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	private void sendStringMessage(Queue queue, final String message) {
		sendStringMessage(queue, message, CALL_ID);
	}

	private void sendStringMessage(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			msg.setText(message);
			if (callId != null)
				msg.setStringProperty("callId", callId);
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

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue, String callId) {
		Message message = jmsTemplate.receive(queue);
		try {
			String receivedCallId = message.getStringProperty("callId");
			assertThat(receivedCallId, is(callId));
			return (T) jmsTemplate.getMessageConverter().fromMessage(message);
		} catch (JMSException e) {
			fail(e);
			return null;
		}
	}

	private void verifyAllStubs(int count) {
		verify(count, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(count, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(count, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(count, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	private String henvendelseReplace(String henvendelse) {
		return henvendelse.replaceAll("\r", "").replaceAll("\t", "")
				.replaceFirst("(?<=ferdigstiltDato>).+(?=</ferdigstiltDato)", "")
				.replaceFirst("(?<=varselbestillingId>).+(?=</varselbestillingId)", "");
	}

	private String varselReplace(String varsel) {
		// value elementer med bindestrek (-) dekker FerdigstiltDato og VarselbestillingsId, som genereres per kall
		return varsel.replaceAll("\r", "").replaceAll("\t", "")
				.replaceFirst("(?<=varselbestillingId>).+(?=</varselbestillingId)", "")
				.replaceAll("(?<=value>).*-.*(?=</value)", "");
	}
}



