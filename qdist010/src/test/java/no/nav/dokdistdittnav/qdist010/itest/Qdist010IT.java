package no.nav.dokdistdittnav.qdist010.itest;

import no.nav.dokdistdittnav.qdist010.itest.config.ApplicationTestConfig;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;


@ActiveProfiles("itest")
class Qdist010IT extends ApplicationTestConfig {

	private static final String FORSENDELSE_ID = "33333";
	private static final String FORSENDELSE_PATH = "/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT" + "&varselStatus=OPPRETTET";
	private static String CALL_ID;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Inject
	private Queue qdist010;

	@Inject
	private Queue qdist010FunksjonellFeil;

	@Inject
	private Queue backoutQueue;

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();
	}

	@Test
	void shouldProcessForsendelse() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put(FORSENDELSE_PATH)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
			verify(1, putRequestedFor(urlEqualTo(FORSENDELSE_PATH)));
		});

		verifyAllStubs(1);
	}

	@Test
	void oppretteOppgaveWhenForsendelseDistribusjonTypeIsVedtak() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/forsendelse_distribusjontype_vedtak.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put(FORSENDELSE_PATH)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
			verify(1, putRequestedFor(urlEqualTo(FORSENDELSE_PATH)));
		});
	}

	@Test
	void sendBeskjedWhenForsendelseDistribusjonTypeIsNull() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/forsendelse_distribusjontype_null.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put(FORSENDELSE_PATH)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
			verify(1, putRequestedFor(urlEqualTo(FORSENDELSE_PATH)));
		});
	}

	@Test
	void shouldThrowForsendelseManglerPaakrevdHeaderFunctionalExceptionManglerCallId() throws Exception {

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"), null);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	void shouldThrowForsendelseManglerPaakrevdHeaderFunctionalExceptionEmptyCallId() throws Exception {

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"), "");

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	void shouldThrowForsendelseManglerForsendelseIdFunctionalExceptionManglerForsendelseId() throws Exception {

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-feilId.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-feilId.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	void shouldThrowForsendelseManglerForsendelseIdFunctionalExceptionTomForsendelseId() throws Exception {

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-tom-forsendelseId.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-tom-forsendelseId.xml"));
		});

		verifyAllStubs(0);
	}

	@Test
	void shouldThrowRdist001HentForsendelseFunctionalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil, CALL_ID);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	void shouldThrowRdist001HentForsendelseTechnicalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue, CALL_ID);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(0, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")));
	}

	@Test
	void shouldThrowRdist001HentForsendelseFunctionalExceptionUtenArkivInformasjon() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/getForsendelse_utenArkivInformasjon.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}

	@Test
	void shouldThrowInvalidForsendelseStatusException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/getForsendelse_oversendtForsendelseStatus.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, putRequestedFor(urlEqualTo(FORSENDELSE_PATH)));
	}

	@Test
	void shouldThrowTkat020FunctionalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}

	@Test
	void shouldThrowTkat020FunctionalExceptionUtenDokumentProduksjonsInfo() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}

	@Test
	void shouldThrowRdist001OppdaterForsendelseStatusFunctionalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=EKSPEDERT")
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010FunksjonellFeilQueue = receive(qdist010FunksjonellFeil);
			assertNotNull(resultOnQdist010FunksjonellFeilQueue);
			assertEquals(resultOnQdist010FunksjonellFeilQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verifyAllStubs(1);
	}

	@Test
	void shouldThrowRdist001OppdaterForsendelseStatusTechnicalException() throws Exception {
		stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(testUtils.classpathToString("__files/rdist001/getForsendelse_withAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
		stubFor(put(FORSENDELSE_PATH)
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist010, testUtils.classpathToString("qdist010/qdist010-happy.xml"));

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist010BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist010BackoutQueue);
			assertEquals(resultOnQdist010BackoutQueue, testUtils.classpathToString("qdist010/qdist010-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(3, putRequestedFor(urlEqualTo(FORSENDELSE_PATH)));
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
		verify(count, putRequestedFor(urlEqualTo(FORSENDELSE_PATH)));
	}

}



