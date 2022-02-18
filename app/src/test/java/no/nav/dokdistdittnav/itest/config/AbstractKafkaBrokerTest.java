package no.nav.dokdistdittnav.itest.config;


import no.nav.dokdistdittnav.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {Application.class, CustomAvroSerializer.class, KafkaTestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
		partitions = 1,
		controlledShutdown = true,
		brokerProperties = {
				"listeners=PLAINTEXT://127.0.0.1:60172",
				"port=60172",
				"offsets.topic.replication.factor=1",
				"transaction.state.log.replication.factor=1",
				"transaction.state.log.min.isr=1"
		}
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class AbstractKafkaBrokerTest {
	@Autowired
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	public EmbeddedKafkaBroker kafkaEmbedded;
}
