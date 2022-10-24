package no.nav.dokdistdittnav;

import no.nav.dokdistdittnav.config.kafka.KafkaConfig;
import no.nav.dokdistdittnav.azure.AzureProperties;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.config.properties.MqGatewayAlias;
import no.nav.dokdistdittnav.config.properties.DokdistDittnavServiceuser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@Import({
		CoreConfig.class,
		KafkaConfig.class
})
@EnableConfigurationProperties({
		DokdistDittnavServiceuser.class,
		DokdistdittnavProperties.class,
		AzureProperties.class,
		MqGatewayAlias.class
})
public class Application {
	public static void main(String[] args) {
		// Hindre at passordet lekker ut i loggene.
		System.setProperty("javax.net.ssl.keyStorePassword", System.getenv("SRVDOKDISTDITTNAVCERT_PASSWORD"));
		SpringApplication.run(Application.class, args);
	}
}
