package no.nav.dokdistdittnav.qdist010.itest.config;


import no.nav.dokdistdittnav.config.alias.BrukernotifikasjonTopic;
import no.nav.dokdistdittnav.config.alias.MqGatewayAlias;
import no.nav.dokdistdittnav.config.alias.ServiceuserAlias;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		BrukernotifikasjonTopic.class,
		MqGatewayAlias.class
})
@Import({
		JmsItestConfig.class,
		KafkaTestConfig.class,
		CustomAvroSerializer.class
})
@EnableAutoConfiguration
@ComponentScan(basePackages = "no.nav.dokdistdittnav")
public class ApplicationTestConfig {
}
