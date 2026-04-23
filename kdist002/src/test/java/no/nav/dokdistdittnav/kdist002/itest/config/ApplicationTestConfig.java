package no.nav.dokdistdittnav.kdist002.itest.config;

import no.nav.dokdistdittnav.azure.AzureProperties;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.config.properties.MqGatewayAlias;
import no.nav.dokdistdittnav.kafka.SerializerPerKafkaTopicConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("itest")
@EnableResilientMethods
@EnableConfigurationProperties({
		DokdistDittnavServiceuser.class,
		DokdistdittnavProperties.class,
		AzureProperties.class,
		MqGatewayAlias.class
})
@Import({
		JmsItestConfig.class,
		SerializerPerKafkaTopicConfiguration.class
})
@EmbeddedKafka(partitions = 1)
@EnableAutoConfiguration
@ComponentScan(basePackages = "no.nav.dokdistdittnav")
@SpringBootTest(classes = ApplicationTestConfig.class, webEnvironment = RANDOM_PORT)
@EnableWireMock
public class ApplicationTestConfig {
}
