package no.nav.dokdistdittnav.kdist001.itest.config;

import no.nav.dokdistdittnav.azure.AzureProperties;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.config.properties.MqGatewayAlias;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.retry.annotation.EnableRetry;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({
		DokdistDittnavServiceuser.class,
		DokdistdittnavProperties.class,
		AzureProperties.class,
		MqGatewayAlias.class
})
@Import({
		JmsItestConfig.class
})
@EmbeddedKafka(
		partitions = 1,
		brokerProperties = {
				"listeners=PLAINTEXT://127.0.0.1:60172"
		}
)
@EnableAutoConfiguration
@ComponentScan(basePackages = "no.nav.dokdistdittnav")
@SpringBootTest(
		classes = {
				ApplicationTestConfig.class
		},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
public class ApplicationTestConfig {
}
