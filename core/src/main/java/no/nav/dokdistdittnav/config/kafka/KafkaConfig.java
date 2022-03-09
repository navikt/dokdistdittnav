package no.nav.dokdistdittnav.config.kafka;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;

@EnableConfigurationProperties(KafkaProperties.class)
@Configuration
public class KafkaConfig {

	private final KafkaProperties properties;

	@Inject
	public KafkaConfig(KafkaProperties properties) {
		this.properties = properties;
	}

	@Bean
	public CamelKafkaProperties camelKafkaProperties() {
		return new CamelKafkaProperties(properties);
	}
}
