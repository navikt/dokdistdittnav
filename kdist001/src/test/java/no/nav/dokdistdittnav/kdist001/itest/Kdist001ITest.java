package no.nav.dokdistdittnav.kdist001.itest;

import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist001.itest.config.ApplicationTestConfig;
import no.nav.safselvbetjening.schemas.HoveddokumentLest;
import no.nav.tms.varsel.builder.BuilderEnvironment;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
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
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.kafka.test.utils.ContainerTestUtils.waitForAssignment;

@ActiveProfiles("itest")
public class Kdist001ITest extends ApplicationTestConfig {

	private static final String DOKUMENTINFO_ID = "236434";
	private static final String JOURNALPOST_ID = "153781366";
	private static final String FORSENDELSE_ID = "1720847";
	private static final String BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String PROPERTY_JOURNALPOST = "journalpostId";

	private static final String DOKDISTDITTNAV_LESTAVMOTTAKER_TOPIC = "teamdokumenthandtering.privat-dokdistdittnav-lestavmottaker-test";
	private static final String INAKTIVER_VARSEL_TOPIC = "min-side.aapen-brukervarsel-v1-test";

	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;
	private static final String FINNFORSENDELSE_URL = "/rest/v1/administrerforsendelse/finnforsendelse/%s/%s";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String OPPDATERDISTRIBUSJONSINFO_URL = "/rest/journalpostapi/v1/journalpost/%s/oppdaterDistribusjonsinfo";

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	private BlockingQueue<ConsumerRecord<String, Object>> records;

	private KafkaMessageListenerContainer<String, String> container;

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@BeforeEach
	public void setUp() {
		DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
		ContainerProperties containerProperties = new ContainerProperties(DOKDISTDITTNAV_LESTAVMOTTAKER_TOPIC, INAKTIVER_VARSEL_TOPIC);
		container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
		records = new LinkedBlockingQueue<>();
		container.setupMessageListener((MessageListener<String, Object>) e -> records.add(e));
		container.start();
		waitForAssignment(container, embeddedKafkaBroker.getTopics().size() * embeddedKafkaBroker.getPartitionsPerTopic());

		stubAzure();

		// Brukt av no.nav.tms.varsel sin java-builder
		BuilderEnvironment.extend(Map.of(
				"NAIS_CLUSTER_NAME", "test-fss",
				"NAIS_NAMESPACE", "teamdokumenthandtering",
				"NAIS_APP_NAME", "dokdistdittnav")
		);
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
	public void skalBehandleHoveddokumentLestMelding() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("rdist001/hentforsendelse_happy.json", OK.value());
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		putMessageOnKafkaTopic(lagHoveddokumentLest());

		await().atMost(10, SECONDS).untilAsserted(() -> {
			assertThat(records.stream())
					.filteredOn(it -> INAKTIVER_VARSEL_TOPIC.equals(it.topic()))
					.hasSize(1)
					.extracting(ConsumerRecord::key, ConsumerRecord::value)
					.asString()
					.contains(BESTILLINGSID, "\"varselId\":\"%s\"".formatted(BESTILLINGSID));
		});

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
		stubGetHentForsendelse("rdist001/hentforsendelse_ingen_innhold.json", NO_CONTENT.value());
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
			"rdist001/hentforsendelse_varselstatus_ferdigstilt.json",
			"rdist001/hentforsendelse_varselstatus_feilet.json"
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
		stubGetHentForsendelse("rdist001/hentforsendelse_hoveddokument_mangler.json", OK.value());
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
		stubGetHentForsendelse("rdist001/hentforsendelse_feil_dokumentinfoid.json", OK.value());
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
		stubGetHentForsendelse("rdist001/hentforsendelse_distribusjonskanal_sdp.json", OK.value());
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
						.withBodyFile(responsebody)));
	}

	void stubGetFinnForsendelse() {
		stubFor(get(FINNFORSENDELSE_URL.formatted(PROPERTY_JOURNALPOST, JOURNALPOST_ID))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rdist001/finnforsendelse_happy.json")));
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

}