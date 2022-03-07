package no.nav.dokdistdittnav.config.kafka;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CamelKafkaProperties {

	private final KafkaProperties kafkaProperties;

	public CamelKafkaProperties(KafkaProperties kafkaProperties) {
		this.kafkaProperties = kafkaProperties;
	}

	public String buildKafkaUrl(String dokdistdittnavTopic, String configProperties) {
		KafkaProperties.Ssl ssl = kafkaProperties.getSsl();

		return "kafka:" + dokdistdittnavTopic +
				"?brokers=" + join(",", kafkaProperties.getBootstrapServers()) +
				"&securityProtocol=" + kafkaProperties.getSecurity().getProtocol() +
				// truststore
				appendIfNotBlank("&sslTruststoreType", ssl.getTrustStoreType()) +
				appendIfNotBlank("&sslTruststoreLocation", ssl.getTrustStoreLocation()) +
				appendIfNotBlank("&sslTruststorePassword", ssl.getTrustStorePassword()) +
				// keystore
				appendIfNotBlank("&sslKeystoreType", ssl.getKeyStoreType()) +
				appendIfNotBlank("&sslKeystoreLocation", ssl.getKeyStoreLocation()) +
				appendIfNotBlank("&sslKeystorePassword", ssl.getKeyStorePassword()) +
				appendIfNotBlank("&sslKeyPassword", ssl.getKeyPassword()) +
				//producer or consumer
				configProperties +
				// confluent
				"&schemaRegistryURL=" + kafkaProperties.getProperties().get(SCHEMA_REGISTRY_URL_CONFIG) +
				"&specificAvroReader=true" +
				"&additionalProperties.schema.registry.url=" + kafkaProperties.getProperties().get(SCHEMA_REGISTRY_URL_CONFIG) +
				"&additionalProperties.basic.auth.credentials.source=" + kafkaProperties.getProperties().get(BASIC_AUTH_CREDENTIALS_SOURCE) +
				"&additionalProperties.basic.auth.user.info=" + kafkaProperties.getProperties().get(USER_INFO_CONFIG);
	}

	public String kafkaConsumer() {
		KafkaProperties.Consumer consumer = kafkaProperties.getConsumer();
		// consumer
		return "&groupId=" + consumer.getGroupId() +
				"&keyDeserializer=" + consumer.getKeyDeserializer().getName() +
				"&valueDeserializer=" + consumer.getValueDeserializer().getName() +
				"&maxPollRecords=" + consumer.getMaxPollRecords() +
				"&heartbeatIntervalMs=" + consumer.getHeartbeatInterval().toMillis() +
				"&autoOffsetReset=" + consumer.getAutoOffsetReset() +
				"&autoCommitEnable=" + consumer.getEnableAutoCommit() +
				"&heartbeatIntervalMs=" + consumer.getHeartbeatInterval() +
				// custom
				"&allowManualCommit=true" +
				"&bridgeErrorHandler=true" +
				"&breakOnFirstError=true";
	}

	public String kafkaProducer() {
		KafkaProperties.Producer producer = kafkaProperties.getProducer();
		// producer
		return "&requestRequiredAcks=" + producer.getAcks() +
				"&keySerializer=" + producer.getKeySerializer().getName() +
				"&valueSerializer=" + producer.getValueSerializer().getName() +
				"&retries=" + producer.getRetries() +
				"&enableIdempotence=true" +
				"&additionalProperties.max.in.flight.requests.per.connection=1";
	}

	private String appendIfNotBlank(String config, Resource value) {
		try {
			return value == null ? "" : appendIfNotBlank(config, value.getFile().getAbsolutePath());
		} catch (IOException e) {
			throw new ApplicationContextException("Kunne ikke konfigurere config=" + config, e);
		}
	}

	private String appendIfNotBlank(String config, String value) {
		return isBlank(value) ? "" : config + "=" + value;
	}

	public String getGroupId() {
		return kafkaProperties.getConsumer().getGroupId();
	}
}
