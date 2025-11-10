package no.nav.dokdistdittnav.kafka;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.RoutingKafkaTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Configuration
public class CustomKafkaTemplate {

	@Bean
	public RoutingKafkaTemplate routingTemplate(ProducerFactory<Object, Object> defaultProducerFactory) {

		// Clone the PF with a different Serializer, register with Spring for shutdown
		Map<String, Object> configs = new HashMap<>(defaultProducerFactory.getConfigurationProperties());
		configs.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configs.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		DefaultKafkaProducerFactory<Object, Object> minSideAapenBrukervarselProducerFactory = new DefaultKafkaProducerFactory<>(configs);

		Map<Pattern, ProducerFactory<Object, Object>> map = new LinkedHashMap<>();
		map.put(Pattern.compile("min-side.aapen-brukervarsel-v1.*"), minSideAapenBrukervarselProducerFactory);
		map.put(Pattern.compile("min-side.aapen-brukernotifikasjon-done-v1.*"), defaultProducerFactory);
		map.put(Pattern.compile("teamdokumenthandtering.*"), defaultProducerFactory);

		return new RoutingKafkaTemplate(map);
	}

}