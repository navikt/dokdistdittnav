package no.nav.dokdistdittnav.kdist002.itest;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistdittnav.kafka.KafkaEventProducer;
import no.nav.dokdistdittnav.kdist002.itest.config.ApplicationTestConfig;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.tms.varsel.builder.BuilderEnvironment;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdittnav.constants.DomainConstants.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.kdist002.Kdist002Route.TMS_EKSTERN_VARSLING;
import static no.nav.dokdistdittnav.kdist002.TestUtils.MELDING;
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

	private static final String DOKNOTIFIKASJON_BESTILLINGSID = "B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String MINSIDE_FORSENDELSE_ID = "1720847";
	private static final String MINSIDE_BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String PRINT_FORSENDELSE_ID = "33333";

	private static final String FEIL_BESTILLERID_DOKDISTDPI = "dokdistdpi";
	private static final String RIKTIG_BESTILLERID_DOKDISTDITTNAV = "dokdistdittnav";

	private static final String DOKNOTIFIKASJON_STATUS_TOPIC = "teamdokumenthandtering.aapen-dok-notifikasjon-status-test";
	private static final String INAKTIVER_VARSEL_TOPIC = "min-side.aapen-brukervarsel-v1-test";

	private static final String OPPRETTFORSENDELSE_URL = "/rest/v1/administrerforsendelse";
	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/" + MINSIDE_FORSENDELSE_ID;
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
	void setUp() {
		DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
		ContainerProperties containerProperties = new ContainerProperties(DOKNOTIFIKASJON_STATUS_TOPIC, INAKTIVER_VARSEL_TOPIC);
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

	@ParameterizedTest
	@ValueSource(strings = {RIKTIG_BESTILLERID_DOKDISTDITTNAV, TMS_EKSTERN_VARSLING})
	public void skalFeilregistrereMinSideForsendelseOgLageNyForsendelse(String bestillerId) {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json");
		stubPostOpprettForsendelse();
		stubPutFeilregistrerforsendelse();
		stubPutOppdaterForsendelse();

		sendMessageToDoknotifikasjonStatusTopic(lagDoknotifikasjonStatusMelding(bestillerId, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertThat(message).contains(PRINT_FORSENDELSE_ID);

			assertThat(records.stream())
					.filteredOn(it -> INAKTIVER_VARSEL_TOPIC.equals(it.topic()))
					.hasSize(1)
					.extracting(ConsumerRecord::key, ConsumerRecord::value)
					.asString()
					.contains(MINSIDE_BESTILLINGSID, "\"varselId\":\"%s\"".formatted(MINSIDE_BESTILLINGSID));
		});

		// Opprett ny forsendelse, feilregistrer den gamle, og oppdater forsendelsestatus på forsendelse til KLAR_FOR_DIST
		verify(1, postRequestedFor(urlEqualTo(OPPRETTFORSENDELSE_URL)));
		verify(1, putRequestedFor(urlEqualTo(FEILREGISTRERFORSENDELSE_URL)));
		verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void skalAvslutteBehandlingAvMeldingHvisBestillerIdErUlikDittnavEllerTmsEksternVarsling() {
		sendMessageToDoknotifikasjonStatusTopic(lagDoknotifikasjonStatusMelding(FEIL_BESTILLERID_DOKDISTDPI, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() ->
				verify(0, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGS_ID, MINSIDE_BESTILLINGSID))))
		);
	}

	@Test
	public void shouldLogWhenVarselstatusIsNotEqualToOPPRETTET() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json");

		sendMessageToDoknotifikasjonStatusTopic(lagDoknotifikasjonStatusMelding(RIKTIG_BESTILLERID_DOKDISTDITTNAV, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGS_ID, MINSIDE_BESTILLINGSID))));
			verify(getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		});
	}

	@Test
	public void shouldUpdateDistInfo() {
		stubGetFinnForsendelse();
		stubNotifikasjonInfo();
		stubUpdateVarselInfo();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json");
		stubPutOppdaterForsendelse();

		sendMessageToDoknotifikasjonStatusTopic(lagDoknotifikasjonStatusMeldingMedDistribusjonsId(RIKTIG_BESTILLERID_DOKDISTDITTNAV, OVERSENDT.name(), null));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGS_ID, MINSIDE_BESTILLINGSID))));
			verify(1, putRequestedFor((urlEqualTo(OPPDATERVARSELINFO_URL))));
		});
	}

	@Test
	public void shouldLogAndAvsluttBehandlingHvisForsendelseStatusErFEILET() {
		stubGetFinnForsendelse();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-forsendelsestatus-feilet.json");

		sendMessageToDoknotifikasjonStatusTopic(lagDoknotifikasjonStatusMelding(RIKTIG_BESTILLERID_DOKDISTDITTNAV, FEILET.name()));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGS_ID, MINSIDE_BESTILLINGSID))));
			verify(getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERVARSELINFO_URL)));
		});
	}

	@ParameterizedTest
	@MethodSource
	public void shouldRetryForFerdigstilteNotifikasjonerSaaLengeSendtDatoMangler(String melding, int antallKall) {
		stubGetFinnForsendelse();
		stubUpdateVarselInfo();
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json");
		stubPutOppdaterForsendelse();
		stubNotifikasjonInfoHvorSendtDatoManglerToGanger();

		var doknotifikasjonStatus = lagDoknotifikasjonStatusMeldingMedDistribusjonsId(RIKTIG_BESTILLERID_DOKDISTDITTNAV, FERDIGSTILT.name(), null);
		doknotifikasjonStatus.setMelding(melding);

		sendMessageToDoknotifikasjonStatusTopic(doknotifikasjonStatus);

		await().atMost(10, SECONDS).untilAsserted(() -> {
					verify(antallKall, getRequestedFor(urlEqualTo(NOTIFIKASJONINFO_URL)));
					verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
				}
		);
	}

	private static Stream<Arguments> shouldRetryForFerdigstilteNotifikasjonerSaaLengeSendtDatoMangler() {
		return Stream.of(
				Arguments.of("notifikasjon sendt via sms", 3),
				Arguments.of("renotifikasjon er stanset", 1)
		);
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

	private void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	private void stubGetHentForsendelse(String responsebody) {
		stubFor(get(HENTFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responsebody))));
	}

	private void stubGetFinnForsendelse() {
		stubFor(get(format(FINNFORSENDELSE_URL, PROPERTY_BESTILLINGS_ID, MINSIDE_BESTILLINGSID))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/rdist001/finnForsendelseresponse-happy.json"))));
	}

	private void stubPutOppdaterForsendelse() {
		stubFor(put(OPPDATERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubPutFeilregistrerforsendelse() {
		stubFor(put(FEILREGISTRERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubUpdateVarselInfo() {
		stubFor(put(OPPDATERVARSELINFO_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubNotifikasjonInfo() {
		stubFor(get(NOTIFIKASJONINFO_URL)
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(OK.value())
						.withBody(classpathToString("__files/rnot001/doknot-happy.json"))));
	}

	private void stubPostOpprettForsendelse() {
		stubFor(post(urlEqualTo(OPPRETTFORSENDELSE_URL))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(OK.value())
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

	private DoknotifikasjonStatus lagDoknotifikasjonStatusMelding(String bestillerId, String status) {
		return lagDoknotifikasjonStatusMeldingMedDistribusjonsId(bestillerId, status, 1L);
	}

	private DoknotifikasjonStatus lagDoknotifikasjonStatusMeldingMedDistribusjonsId(String bestillerId, String status, Long distribusjonsId) {
		return DoknotifikasjonStatus.newBuilder()
				.setBestillerId(bestillerId)
				.setBestillingsId(DOKNOTIFIKASJON_BESTILLINGSID)
				.setStatus(status)
				.setDistribusjonId(distribusjonsId)
				.setMelding(MELDING)
				.build();
	}

}