package no.nav.dokdistdittnav;

import no.nav.dokdistdittnav.config.kafka.KafkaConfig;
import no.nav.dokdistdittnav.config.properties.AdministrerforsendelseApi;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.config.properties.MqGatewayAlias;
import no.nav.dokdistdittnav.config.properties.ServiceuserAlias;
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
		ServiceuserAlias.class,
		DokdistdittnavProperties.class,
		AdministrerforsendelseApi.class,
		MqGatewayAlias.class
})
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
