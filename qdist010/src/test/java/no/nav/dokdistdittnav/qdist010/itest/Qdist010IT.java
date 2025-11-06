package no.nav.dokdistdittnav.qdist010.itest;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistdittnav.qdist010.config.ApplicationTestConfig;
import no.nav.dokdistdittnav.qdist010.minsidevarsling.VarselService;
import no.nav.tms.varsel.builder.BuilderEnvironment;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles("itest")
class Qdist010IT extends ApplicationTestConfig {

	private static final String FORSENDELSE_ID = "33333";
	private static final String OPPDATERFORSENDELSE_PATH = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String HENTFORSENDELSE_PATH = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;

	private static final ZoneId OSLO_ZONE = ZoneId.of("Europe/Oslo");
	private static String CALL_ID;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist010;

	@Autowired
	private Queue qdist010FunksjonellFeil;

	@Autowired
	private Queue qdist010UtenforKjernetid;

	@Autowired
	private Queue qdist010Bq;

	@Autowired
	private VarselService varselService;

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();

		// Brukt av no.nav.tms.varsel sin java-builder
		BuilderEnvironment.extend(Map.of(
				"NAIS_CLUSTER_NAME", "test-fss",
				"NAIS_NAMESPACE", "teamdokumenthandtering",
				"NAIS_APP_NAME", "dokdistdittnav")
		);

		stubAzure();
	}

	@Bean
	public Clock clock() {
		//15.30.00 -> UTC
		LocalTime morgen = LocalTime.of(13, 30, 0);
		LocalDate today = LocalDate.now(OSLO_ZONE);
		LocalDateTime todayMidnight = LocalDateTime.of(today, morgen);
		return Clock.fixed(todayMidnight.toInstant(UTC), OSLO_ZONE);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"rdist001/hentforsendelse_happy.json",
			"rdist001/hentforsendelse_distribusjontype_vedtak.json",
			"rdist001/hentforsendelse_distribusjontype_null.json"
	})
	void skalPublisereOppgaveNaarDistribusjonstypeErVedtakViktigEllerNull(String filnavn) throws Exception {
		stubHentForsendelse(OK, filnavn);
		stubPutOppdaterForsendelse(OK);

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> verifyAllStubs(1));
	}

	@Test
	void skalPublisereBeskjedNaarDistribusjonstypeErAnnet() throws Exception {
		stubHentForsendelse(OK, "rdist001/hentforsendelse_distribusjontype_annet.json");
		stubPutOppdaterForsendelse(OK);

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> verifyAllStubs(1));
	}

	@Test
	void skalHavnePaaFunksjonellBoqHvisCallIdHeaderErTom() throws Exception {
		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"), "");

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	void skalHavnePaaFunksjonellBoqHvisForsendelseIdErNull() throws Exception {
		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-feilId.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-feilId.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	void skalHavnePaaFunksjonellBoqHvisForsendelseIdErTom() throws Exception {
		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-tom-forsendelseId.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-tom-forsendelseId.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	void skalHavnePaaFunksjonellBoqHvisForsendelsestatusErUlikKlarForDist() throws Exception {
		stubHentForsendelse(OK, "rdist001/hentforsendelse_forsendelsestatus_ulik_klarfordist.json");

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil, CALL_ID);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
		verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_PATH)));
	}

	@Test
	void skalHavnePaaFunksjonellBoqHvisForsendelseIkkeErArkivertIJoark() throws Exception {
		stubHentForsendelse(OK, "rdist001/hentforsendelse_ikke_arkivert_i_joark.json");

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil, CALL_ID);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
		verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_PATH)));
	}

	@Test
	void skalHavnePaaFunksjonellBoqHvis404FraHentForsendelse() throws Exception {
		stubHentForsendelse(NOT_FOUND, "");

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil, CALL_ID);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
		verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_PATH)));
	}

	@Test
	void skalHavnePaaTekniskBoqHvis500FraHentForsendelse() throws Exception {
		stubHentForsendelse(INTERNAL_SERVER_ERROR, "");

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(qdist010Bq, CALL_ID);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
		verify(0, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_PATH)));
	}

	@Test
	void skalHavnePaaFunksjonellBoqHvis404FraOppdaterForsendelse() throws Exception {
		stubHentForsendelse(OK, "rdist001/hentforsendelse_happy.json");
		stubPutOppdaterForsendelse(NOT_FOUND);

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs(1);
	}

	@Test
	void skalHavnePaaTekniskBoqHvis500FraOppdaterForsendelse() throws Exception {
		stubHentForsendelse(OK, "rdist001/hentforsendelse_happy.json");
		stubPutOppdaterForsendelse(INTERNAL_SERVER_ERROR);

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(qdist010Bq);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
		verify(3, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_PATH)));
	}

	@Test
	void skalHavnePaaUtenforKjernetidBoqHvisKlokkenErFoerKjernetid() throws Exception {
		//05.00.00 -> UTC
		LocalTime morgen = LocalTime.of(3, 0, 0);
		LocalDate today = LocalDate.now(OSLO_ZONE);
		LocalDateTime todayMidnight = LocalDateTime.of(today, morgen);
		Clock fixedClock = Clock.fixed(todayMidnight.toInstant(UTC), OSLO_ZONE);
		ReflectionTestUtils.setField(varselService, "clock", fixedClock);

		stubHentForsendelse(OK, "rdist001/hentforsendelse_distribusjonstidspunkt_kjernetid.json");
		stubPutOppdaterForsendelse(OK);

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010UtenforKjernetidQueue = receive(qdist010UtenforKjernetid);
			assertNotNull(resultOnQdist010UtenforKjernetidQueue);
			assertEquals(resultOnQdist010UtenforKjernetidQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
	}

	@Test
	void skalHavnePaaUtenforKjernetidBoqHvisKlokkenErEtterKjernetid() throws Exception {
		//23.30.00 -> UTC
		LocalTime morgen = LocalTime.of(23, 30, 0);
		LocalDate today = LocalDate.of(2022, 4, 29);
		LocalDateTime todayMidnight = LocalDateTime.of(today, morgen);
		Clock fixedClock = Clock.fixed(todayMidnight.atZone(OSLO_ZONE).toInstant(), OSLO_ZONE);
		ReflectionTestUtils.setField(varselService, "clock", fixedClock);

		stubHentForsendelse(OK, "rdist001/hentforsendelse_distribusjonstidspunkt_kjernetid.json");
		stubPutOppdaterForsendelse(OK);

		sendStringMessage(qdist010, classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist010UtenforKjernetidQueue = receive(qdist010UtenforKjernetid);
			assertNotNull(resultOnQdist010UtenforKjernetidQueue);
			assertEquals(resultOnQdist010UtenforKjernetidQueue, classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
	}

	private void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	private void stubHentForsendelse(HttpStatus status, String responseBodyFile) {
		stubFor(get(HENTFORSENDELSE_PATH)
				.willReturn(aResponse()
						.withStatus(status.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(responseBodyFile)));
	}

	private void stubPutOppdaterForsendelse(HttpStatus  httpStatus) {
		stubFor(put(OPPDATERFORSENDELSE_PATH)
				.willReturn(aResponse()
						.withStatus(httpStatus.value())));
	}

	private void sendStringMessage(Queue queue, final String message) {
		sendStringMessage(queue, message, CALL_ID);
	}

	private void sendStringMessage(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = session.createTextMessage();
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
		verify(count, getRequestedFor(urlEqualTo(HENTFORSENDELSE_PATH)));
		verify(count, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_PATH)));
	}

	private String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}
}
