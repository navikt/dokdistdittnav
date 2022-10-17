package no.nav.dokdistdittnav.kdist002.itest;

import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist002.itest.config.ApplicationTestConfig;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.jms.Queue;
import javax.xml.bind.JAXBElement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.INFO;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.OVERSENDT;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles("itest")
public class Kdist002ITest extends ApplicationTestConfig {

	private static final String FORSENDELSE_ID = "1720847";
	private static final String NY_FORSENDELSE_ID = "33333";
	private static final String DOKNOTIFIKASJON_BESTILLINGSID = "B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String DOKNOTIFIKASJON_STATUS_TOPIC = "aapen-dok-notifikasjon-status";
	private static final String MELDING = "Altinn feilet";
	private static final String DOKDISTDPI = "dokdistdpi";
	private static final String DOKDISTDITTNAV = "dokdistdittnav";

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	@Autowired
	private Queue qdist009;

	@Autowired
	private JmsTemplate jmsTemplate;

	private BlockingQueue<ConsumerRecord<String, Object>> records;

	private KafkaMessageListenerContainer<String, String> container;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@BeforeAll
	void setupKafkaListener(){
		DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
		ContainerProperties containerProperties = new ContainerProperties("aapen-dok-notifikasjon-status");
		container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
		records = new LinkedBlockingQueue<>();
		container.setupMessageListener((MessageListener<String, Object>) e -> records.add(e));
	}

	@BeforeEach
	void setUp() {
		container.start();
		ContainerTestUtils. waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	@AfterEach
	void steardown(){
		container.stop();
	}

	private Map<String, Object> getConsumerProperties() {
		Map<String, Object> map = new HashMap<>();
		map.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		map.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer");
		map.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
		map.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10");
		map.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "60000");
		map.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		map.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		map.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		return map;
	}

	@Test
	public void shouldFeilRegistrerForsendelseOgOppdaterForsendelse() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", HttpStatus.OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});

		verifyAndCountForsendelse(BESTILLINGSID, KLAR_FOR_DIST.name());
	}

	@Test
	public void shouldAvsluttBehandlingenWhenBestillerIdIsNotDittnavAndStatusIsNotFeilet() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDPI, INFO.name()));
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", HttpStatus.OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + BESTILLINGSID)));
		});
	}

	@Test
	public void shouldAvsluttBehandlingenWhenBestillerIdIsNotDittnavAndStatusFeilet() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDPI, FEILET.name()));
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", HttpStatus.OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + BESTILLINGSID)));
		});
	}

	@Test
	public void shouldLogWhenVarselstatusIsNotEqualToOPPRETTET() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json", FORSENDELSE_ID, OK.value());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + BESTILLINGSID)));
			verify(getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		});
	}

	@Test
	public void shouldUpdateDistInfo() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, OVERSENDT.name(), null));
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubNotifikasjonInfo("__files/rnot001/doknot-happy.json", OK.value());
		stubUpdateVarselInfo();

		await().pollInterval(500, MILLISECONDS).atMost(2, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + BESTILLINGSID)));
			verify(1, putRequestedFor((urlEqualTo("/administrerforsendelse/oppdatervarselinfo"))));
		});
	}

	@Test
	public void shouldLogAndAvsluttBehandlingHvisForsendelseStatusErFEILET() {
		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json", FORSENDELSE_ID, OK.value());

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			ConsumerRecord<String, Object> record = records.poll();
			assertTrue(record != null);
			assertTrue(record.value().toString().contains(MELDING));
			verify(getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + BESTILLINGSID)));
			verify(getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		});
	}

	private void verifyAndCountForsendelse(String bestillingsId, String forsendelseStatus) {
		verify(getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?bestillingsId=" + bestillingsId)));
		verify(getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(postRequestedFor(urlMatching("/administrerforsendelse")));
		verify(putRequestedFor(urlMatching("/administrerforsendelse/feilregistrerforsendelse")));
		verify(putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + NY_FORSENDELSE_ID + "&forsendelseStatus=" + forsendelseStatus)));
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

	private void stubUpdateVarselInfo() {
		stubFor(put("/administrerforsendelse/oppdatervarselinfo")
				.willReturn(aResponse().withStatus(200)));
	}

	private void stubNotifikasjonInfo(String responseBody, int httpStatusValue) {
		stubFor(get("/rest/v1/notifikasjoninfo/B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3")
				.willReturn(aResponse()
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(httpStatusValue)
						.withBody(classpathToString(responseBody))));
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

	public DoknotifikasjonStatus doknotifikasjonStatus(String appnavn, String status, Long distribusjonsId) {
		return DoknotifikasjonStatus.newBuilder()
				.setBestillerId(appnavn)
				.setBestillingsId(DOKNOTIFIKASJON_BESTILLINGSID)
				.setStatus(status)
				.setDistribusjonId(distribusjonsId)
				.setMelding(MELDING)
				.build();
	}

	public DoknotifikasjonStatus doknotifikasjonStatus(String appnavn, String status) {
		return doknotifikasjonStatus(appnavn, status, 1L);
	}
}
