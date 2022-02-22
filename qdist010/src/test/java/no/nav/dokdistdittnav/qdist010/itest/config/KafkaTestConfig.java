package no.nav.dokdistdittnav.qdist010.itest.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.security.auth.SecurityProtocol.PLAINTEXT;

@EnableKafka
@Configuration
@Profile("itest")
public class KafkaTestConfig {

	@Bean
	public KafkaTemplate<Object, Object> kafkaAvroTemplate(ProducerFactory<Object, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	public ProducerFactory<Object, Object> kafkaAvroProducerFactory(KafkaProperties properties) {
		Map<String, Object> producerProperties = properties.buildProducerProperties();
		producerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:60172");
		producerProperties.put(SECURITY_PROTOCOL_CONFIG, PLAINTEXT.name);
		return new DefaultKafkaProducerFactory<>(producerProperties);
	}
}
