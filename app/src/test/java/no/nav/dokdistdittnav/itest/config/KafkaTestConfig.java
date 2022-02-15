package no.nav.dokdistdittnav.itest.config;

import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@EnableKafka
@Configuration
@Profile("itest")
public class KafkaTestConfig {


	@Bean
	public KafkaTemplate<?, ?> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	@Bean
	public ProducerFactory<?, ?> producerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:60172");
		config.put(SCHEMA_REGISTRY_URL_CONFIG, "bogus");
		config.put(KEY_SERIALIZER_CLASS_CONFIG, CustomAvroSerializer.class.getName());
		config.put(VALUE_SERIALIZER_CLASS_CONFIG, CustomAvroSerializer.class.getName());
		config.put("security.protocol", SecurityProtocol.SSL.name);

		return new DefaultKafkaProducerFactory<>(config);
	}

	private KafkaProperties kafkaProperties(){
		return new KafkaProperties();
	}
}
