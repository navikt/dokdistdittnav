package no.nav.dokdistdittnav.kdist002.itest;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist002.itest.config.ApplicationTestConfig;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FEILET;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.FERDIGSTILT;
import static no.nav.dokdistdittnav.kdist002.kodeverk.DoknotifikasjonStatusKode.OVERSENDT;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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

	private static final String ADMINISTRERFORSENDELSE_URL = "/rest/v1/administrerforsendelse";
	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String FINNFORSENDELSE_URL = "/rest/v1/administrerforsendelse/finnforsendelse/%s/%s";
	private static final String OPPDATERVARSELINFO_URL = "/rest/v1/administrerforsendelse/oppdatervarselinfo";
	private static final String FEILREGISTRERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/feilregistrerforsendelse";
	private static final String NOTIFIKASJONINFO_URL = "/rest/v1/notifikasjoninfo/" + DOKNOTIFIKASJON_BESTILLINGSID;

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
	void setUp()  {
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
		map.put(BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		map.put(GROUP_ID_CONFIG, "consumer");
		map.put(ENABLE_AUTO_COMMIT_CONFIG, "true");
		map.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, "10");
		map.put(SESSION_TIMEOUT_MS_CONFIG, "60000");
		map.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		map.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		map.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
		return map;
	}

	@Test
	public void shouldFeilregistrerForsendelseOgOppdaterForsendelse() {
		stubGetFinnForsendelse(OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", OK.value());
		stubPostOpprettForsendelse(OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(OK.value());

		sendMessageToDoknotifikasjonStatusTopic(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() -> {
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

		verify(1, putRequestedFor(urlEqualTo(FEILREGISTRERFORSENDELSE_URL)));
		verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldAvsluttBehandlingenWhenBestillerIdIsNotDittnav() {
		stubGetFinnForsendelse(OK.value());

		sendMessageToDoknotifikasjonStatusTopic(doknotifikasjonStatus(DOKDISTDPI, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() ->
				verify(0, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))))
		);
	}

	@Test
	public void shouldLogWhenVarselstatusIsNotEqualToOPPRETTET() {
		stubGetFinnForsendelse(OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json", OK.value());

		sendMessageToDoknotifikasjonStatusTopic(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))));
			verify(getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		});
	}

	@Test
	public void shouldUpdateDistInfo() {
		stubGetFinnForsendelse(OK.value());
		stubNotifikasjonInfo("__files/rnot001/doknot-happy.json", OK.value());
		stubUpdateVarselInfo();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(OK.value());

		sendMessageToDoknotifikasjonStatusTopic(doknotifikasjonStatus(DOKDISTDITTNAV, OVERSENDT.name(), null));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))));
			verify(1, putRequestedFor((urlEqualTo(OPPDATERVARSELINFO_URL))));
		});
	}

	@Test
	public void shouldLogAndAvsluttBehandlingHvisForsendelseStatusErFEILET() {
		stubGetFinnForsendelse(OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json", OK.value());

		sendMessageToDoknotifikasjonStatusTopic(doknotifikasjonStatus(DOKDISTDITTNAV, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))));
			verify(getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERVARSELINFO_URL)));
		});
	}

	@Test
	public void shouldRetryForFerdigstilteNotifikasjonerSaaLengeSendtDatoMangler() {
		stubGetFinnForsendelse(OK.value());
		stubUpdateVarselInfo();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(OK.value());
		stubNotifikasjonInfoHvorSendtDatoManglerToGanger();

		sendMessageToDoknotifikasjonStatusTopic(doknotifikasjonStatus(DOKDISTDITTNAV, FERDIGSTILT.name(), null));

		await().atMost(10, SECONDS).untilAsserted(() ->
				verify(3, getRequestedFor(urlEqualTo(NOTIFIKASJONINFO_URL))));
	}

	//Scenario hvor returnert notifikasjonInfo mangler sendtDato i kall nr 1 og 2, men inneholder sendtDato i kall nr 3
	private void stubNotifikasjonInfoHvorSendtDatoManglerToGanger() {
		var responseBodyUtenSendtDato = "rnot001/doknot-uten-sendtdato.json";
		var responseBodyMedSendtDato = "rnot001/doknot-happy.json";

		stubFor(get(NOTIFIKASJONINFO_URL)
				.inScenario("SendtDato")
				.whenScenarioStateIs(STARTED)
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(OK.value())
						.withBodyFile(responseBodyUtenSendtDato))
				.willSetStateTo("SendtDatoManglerForAndreGang"));

		stubFor(get(NOTIFIKASJONINFO_URL)
				.inScenario("SendtDato")
				.whenScenarioStateIs("SendtDatoManglerForAndreGang")
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(OK.value())
						.withBodyFile(responseBodyUtenSendtDato))
				.willSetStateTo("SendtDatoOK"));

		stubFor(get(NOTIFIKASJONINFO_URL)
				.inScenario("SendtDato")
				.whenScenarioStateIs("SendtDatoOK")
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(OK.value())
						.withBodyFile(responseBodyMedSendtDato)));
	}

	private void stubGetHentForsendelse(String responsebody, int httpStatusvalue) {
		stubFor(get(HENTFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(httpStatusvalue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responsebody))));
	}

	void stubGetFinnForsendelse(int httpStatusValue) {
		stubFor(get(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGSID, BESTILLINGSID))
				.willReturn(aResponse()
						.withStatus(httpStatusValue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rdist001/finnForsendelseresponse-happy.json"))));
	}

	private void stubPutOppdaterForsendelse(int httpStatusvalue) {
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
		stubFor(get(NOTIFIKASJONINFO_URL)
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(httpStatusValue)
						.withBody(classpathToString(responseBody))));
	}

	private void stubPostOpprettForsendelse(int httpStatusValue) {
		stubFor(post(urlEqualTo(ADMINISTRERFORSENDELSE_URL))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(httpStatusValue)
						.withBody(classpathToString("__files/rdist001/opprettForsendelseResponse-happy.json"))));
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}

	private void sendMessageToDoknotifikasjonStatusTopic(DoknotifikasjonStatus status) {
		kafkaEventProducer.publish(DOKNOTIFIKASJON_STATUS_TOPIC, "key", status);
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
