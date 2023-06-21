package no.nav.dokdistdittnav.kdist002.itest;

import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist002.itest.config.ApplicationTestConfig;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
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
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.INFO;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.OVERSENDT;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.kafka.test.utils.ContainerTestUtils.waitForAssignment;

@ActiveProfiles("itest")
public class Kdist002ITest extends ApplicationTestConfig {

	private static final String FORSENDELSE_ID = "1720847";
	private static final String NY_FORSENDELSE_ID = "33333";
	private static final String DOKNOTIFIKASJON_BESTILLINGSID = "B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String DOKNOTIFIKASJON_STATUS_TOPIC = "aapen-dok-notifikasjon-status";
	private static final String DONE_EVENT_TOPIC = "done-test";
	private static final String MELDING = "Altinn feilet";
	private static final String DOKDISTDPI = "dokdistdpi";
	private static final String DOKDISTDITTNAV = "dokdistdittnav";
	private static final String PROPERTY_BESTILLINGSID = "bestillingsId";

	private static final String DONEEVENT_DITTNAV_BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String DONEEVENT_DITTNAV_FORSENDELSEID = "1720847";
	private static final String DONEEVENT_DITTNAV_MOTTAKERID = "22222222222";

	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String FINNFORSENDELSE_URL = "/rest/v1/administrerforsendelse/finnforsendelse/%s/%s";
	private static final String OPPDATERVARSELINFO_URL = "/rest/v1/administrerforsendelse/oppdatervarselinfo";
	private static final String FEILREGISTRERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/feilregistrerforsendelse";

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

	@BeforeEach
	void setUp() {
		DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
		ContainerProperties containerProperties = new ContainerProperties(DOKNOTIFIKASJON_STATUS_TOPIC, DONE_EVENT_TOPIC);
		container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
		records = new LinkedBlockingQueue<>();
		container.setupMessageListener((MessageListener<String, Object>) e -> records.add(e));
		container.start();
		waitForAssignment(container, embeddedKafkaBroker.getTopics().size() * embeddedKafkaBroker.getPartitionsPerTopic());

		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	@AfterEach
	void teardown() {
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
	public void shouldFeilregistrerForsendelseOgOppdaterForsendelse() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostOpprettForsendelse("__files/rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			//Sjekk at riktig forsendelseId blir sendt til qdist009/print
			String message = receive(qdist009);
			assertThat(message).contains(NY_FORSENDELSE_ID);

			//Sjekk at riktig forsendelseId blir sendt til brukernotifikasjon via kafka hendelse
			assertThat(records.stream())
					.filteredOn(it -> DONE_EVENT_TOPIC.equals(it.topic()))
					.hasSize(1)
					.extracting(ConsumerRecord::key)
					.asString()
					.contains(DONEEVENT_DITTNAV_BESTILLINGSID,
							DONEEVENT_DITTNAV_FORSENDELSEID,
							DONEEVENT_DITTNAV_MOTTAKERID);
		});

		verifyAndCountForsendelse(BESTILLINGSID);
	}

	@Test
	public void shouldAvsluttBehandlingenWhenBestillerIdIsNotDittnavAndStatusIsNotFeilet() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostOpprettForsendelse("__files/rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDPI, INFO.name()));

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() ->
				verify(0, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))))
		);
	}

	@Test
	public void shouldAvsluttBehandlingenWhenBestillerIdIsNotDittnavAndStatusFeilet() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostOpprettForsendelse("__files/rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDPI, FEILET.name()));

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() ->
				verify(0, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))))
		);
	}

	@Test
	public void shouldLogWhenVarselstatusIsNotEqualToOPPRETTET() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json", FORSENDELSE_ID, OK.value());

		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))));
			verify(getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		});
	}

	@Test
	public void shouldUpdateDistInfo() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubNotifikasjonInfo("__files/rnot001/doknot-happy.json", OK.value());
		stubUpdateVarselInfo();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPutOppdaterForsendelse(EKSPEDERT.name(), FORSENDELSE_ID, OK.value());

		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, OVERSENDT.name(), null));

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))));
			verify(1, putRequestedFor((urlEqualTo(OPPDATERVARSELINFO_URL))));
		});
	}

	@Test
	public void shouldLogAndAvsluttBehandlingHvisForsendelseStatusErFEILET() {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json", FORSENDELSE_ID, OK.value());

		sendMessageToTopic(DOKNOTIFIKASJON_STATUS_TOPIC, doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));

		await().pollInterval(500, MILLISECONDS).atMost(10, SECONDS).untilAsserted(() -> {
			ConsumerRecord<String, Object> record = records.poll();
			assertTrue(record != null);
			assertTrue(record.value().toString().contains(MELDING));
			verify(getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))));
			verify(getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		});
	}

	private void verifyAndCountForsendelse(String bestillingsId) {
		verify(getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, bestillingsId))));
		verify(getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(postRequestedFor(urlMatching("/rest/v1/administrerforsendelse")));
		verify(putRequestedFor(urlMatching(FEILREGISTRERFORSENDELSE_URL)));
		verify(putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	private void stubGetHentForsendelse(String responsebody, String forsendelseId, int httpStatusvalue) {
		stubFor(get(HENTFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(httpStatusvalue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responsebody))));
	}

	void stubGetFinnForsendelse(String responseBody, int httpStatusValue) {
		stubFor(get(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))
				.willReturn(aResponse()
						.withStatus(httpStatusValue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responseBody))));
	}

	private void stubPutOppdaterForsendelse(String forsendelseStatus, String forsendelseId, int httpStatusvalue) {
		stubFor(put(OPPDATERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(httpStatusvalue)));

	}

	private void stubPutFeilregistrerforsendelse(int httpStatusValue) {
		stubFor(put(FEILREGISTRERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(httpStatusValue)));
	}

	private void stubUpdateVarselInfo() {
		stubFor(put(OPPDATERVARSELINFO_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubNotifikasjonInfo(String responseBody, int httpStatusValue) {
		stubFor(get("/rest/v1/notifikasjoninfo/B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3")
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(httpStatusValue)
						.withBody(classpathToString(responseBody))));
	}

	private void stubPostOpprettForsendelse(String responseBody, int httpStatusValue) {
		stubFor(post(urlEqualTo("/rest/v1/administrerforsendelse"))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
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
		kafkaEventProducer.publish(topicname, "key", status);
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
