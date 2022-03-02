package no.nav.dokdistdittnav.config.kafka;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import javax.inject.Inject;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static org.apache.kafka.common.security.auth.SecurityProtocol.SSL;

@EnableConfigurationProperties(KafkaProperties.class)
@Configuration
public class KafkaConfig {

	private final KafkaProperties properties;

	@Inject
	public KafkaConfig(KafkaProperties properties) {
		this.properties = properties;
	}

	@Bean
	public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> kafkaProducerFactory, ProducerListener<Object, Object> kafkaProducerListener, ObjectProvider<RecordMessageConverter> messageConverter) {
		KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate(kafkaProducerFactory);
		messageConverter.ifUnique(kafkaTemplate::setMessageConverter);
		kafkaTemplate.setProducerListener(kafkaProducerListener);
		return kafkaTemplate;
	}

	@Bean
	public ProducerFactory<?, ?> kafkaProducerFactory(ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers) {
		Map<String, Object> kafkaProperties = this.properties.buildProducerProperties();
		kafkaProperties.put("security.protocol", SSL.name);
		DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory(this.properties.buildProducerProperties());
		String transactionIdPrefix = this.properties.getProducer().getTransactionIdPrefix();
		if (transactionIdPrefix != null) {
			factory.setTransactionIdPrefix(transactionIdPrefix);
		}

		customizers.orderedStream().forEach((customizer) -> {
			customizer.customize(factory);
		});
		return factory;
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
}
