package no.nav.dokdistdittnav.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class CamelKafkaProperties {

	private final KafkaProperties kafkaProperties;

	public CamelKafkaProperties() {
		this.kafkaProperties = new KafkaProperties();
	}

	public String buildKafkaUrl(String topic) {
		KafkaProperties.Producer producer = kafkaProperties.getProducer();
		KafkaProperties.Ssl ssl = kafkaProperties.getSsl();
		KafkaProperties.Security security = kafkaProperties.getSecurity();

		return "kafka:" + topic +
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
				//security
				appendIfNotBlank("&securityProtocol", security.getProtocol()) +
				// producer
				"&clientId=" + producer.getClientId() +
				"&keySerializer=" + producer.getKeySerializer().getName() +
				"&valueSerializer=" + producer.getValueSerializer().getName() +
				"&reconnectBackoffMs=" + producer.getCompressionType() +
				"&retries=" + producer.getRetries() +
				// confluent
				"&schemaRegistryURL=" + kafkaProperties.getProperties().get(SCHEMA_REGISTRY_URL_CONFIG) +
				"&specificAvroReader=true" +
				"&additionalProperties.schema.registry.url=" + kafkaProperties.getProperties().get(SCHEMA_REGISTRY_URL_CONFIG) +
				"&additionalProperties.basic.auth.credentials.source=" + kafkaProperties.getProperties().get(BASIC_AUTH_CREDENTIALS_SOURCE) +
				"&additionalProperties.basic.auth.user.info=" + kafkaProperties.getProperties().get(USER_INFO_CONFIG);
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
