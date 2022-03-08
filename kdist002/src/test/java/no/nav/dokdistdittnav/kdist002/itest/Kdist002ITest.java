package no.nav.dokdistdittnav.kdist002.itest;

import lombok.SneakyThrows;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist002.itest.config.ApplicationTestConfig;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;

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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles("itest")
@Disabled
public class Kdist002ITest extends ApplicationTestConfig {

	private static final String FORSENDELSE_ID = "1720847";
	private static final String NY_FORSENDELSE_ID = "33333";
	private static final String DOKNOTIFIKASJON_BESTILLINGSID = "B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String DOKNOTIFIKASJON_STATUS_TOPIC = "aapen-dok-notifikasjon-status";
	private static final String MELDING = "Altinn feilet";

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	@Inject
	private Queue qdist009;

	@Inject
	private JmsTemplate jmsTemplate;

	@Test
	public void shouldFeilRegistrerForsendelseOgOppdaterForsendelse() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus());
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", HttpStatus.OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		await().pollInterval(500, MILLISECONDS).atMost(20, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});

		verifyAndCountForsendelse(BESTILLINGSID, KLAR_FOR_DIST.name());
	}

	@Test
	public void shouldLogAndAvsluttBehandlingHvisForsendelseStatusErFEILET() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus());
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json", FORSENDELSE_ID, OK.value());

		await().pollInterval(500, MILLISECONDS).atMost(20, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + BESTILLINGSID)));
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		});
	}

	private void verifyAndCountForsendelse(String bestillingsId, String forsendelseStatus) {
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + bestillingsId)));
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, postRequestedFor(urlMatching("/administrerforsendelse")));
		verify(1, putRequestedFor(urlMatching("/administrerforsendelse/feilregistrerforsendelse")));
		verify(1, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + NY_FORSENDELSE_ID + "&forsendelseStatus=" + forsendelseStatus)));
	}

	private void stubGetHentForsendelse(String responsebody, String forsendelseId, int httpStatusvalue) {
		stubFor(get("/administrerforsendelse/" + forsendelseId).willReturn(aResponse().withStatus(httpStatusvalue)
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBody(classpathToString(responsebody))));
	}

	void stubGetFinnForsendelse(String responseBody, int httpStatusValue) {
		stubFor(get("/administrerforsendelse/finnforsendelse?bestillingsId=" + BESTILLINGSID)
				.willReturn(aResponse().withStatus(httpStatusValue)
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responseBody))));
	}

	private void stubPutOppdaterForsendelse(String forsendelseStatus, String forsendelseId, int httpStatusvalue) {
		stubFor(put("/administrerforsendelse?forsendelseId=" + forsendelseId + "&forsendelseStatus=" + forsendelseStatus)
				.willReturn(aResponse().withStatus(httpStatusvalue)));

	}

	private void stubPutFeilregistrerforsendelse(int httpStatusValue) {
		stubFor(put("/administrerforsendelse/feilregistrerforsendelse")
				.willReturn(aResponse().withStatus(httpStatusValue)));
	}

	private void stubPostPersisterForsendelse(String responseBody, int httpStatusValue) {
		stubFor(post(urlEqualTo("/administrerforsendelse"))
				.willReturn(aResponse()
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(httpStatusValue)
						.withBody(classpathToString(responseBody))));
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}

	private void sendMessageToTopic(String topicname, DoknotifikasjonStatus status) {
		kafkaEventProducer.publish(
				topicname, "key",
				status
		);
	}

	public DoknotifikasjonStatus doknotifikasjonStatus() {
		return DoknotifikasjonStatus.newBuilder()
				.setBestillerId("dokdistdittnav")
				.setBestillingsId(DOKNOTIFIKASJON_BESTILLINGSID)
				.setStatus(FEILET.name())
				.setDistribusjonId(1L)
				.setMelding(MELDING)
				.build();
	}

	@SneakyThrows
	private static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);

	}
}
