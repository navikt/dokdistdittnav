package no.nav.dokdistdittnav.config.kafka;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import javax.inject.Inject;

import static java.time.Duration.ofSeconds;

@EnableKafka
@EnableConfigurationProperties(KafkaProperties.class)
@Configuration
public class KafkaConfig {

	private final KafkaProperties properties;

	@Inject
	public KafkaConfig(KafkaProperties properties) {
		this.properties = properties;
	}

	@Bean("kafkaListenerContainerFactory")
	@Primary
	ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerFactory(
			ConsumerFactory<Object, Object> kafkaConsumerFactory
	) {
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(kafkaConsumerFactory);
		factory.getContainerProperties().setAuthExceptionRetryInterval(ofSeconds(10L));

		factory.setConcurrency(3);
		return factory;
	}

	@Bean
	public CamelKafkaProperties camelKafkaProperties() {
		return new CamelKafkaProperties(properties);
	}
}
