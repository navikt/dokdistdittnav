package no.nav.dokdistdittnav.kdist001.itest.config;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;

import java.util.HashMap;
import java.util.Map;

public class CustomAvroDeserializer extends KafkaAvroDeserializer {

	public CustomAvroDeserializer() {
		super(SerializationUtils.REGISTRY, props());
	}

	private static Map<String, Object> props() {
		Map<String, Object> props = new HashMap<>();
		props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
		props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "bogus");
		return props;
	}

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		configure(null);
	}

	@Override
	protected void configure(KafkaAvroDeserializerConfig config) {
		this.schemaRegistry = SerializationUtils.REGISTRY;
		this.useSpecificAvroReader = true;
		KafkaAvroDeserializerConfig a = new KafkaAvroDeserializerConfig(props());
		configureClientProperties(a, new AvroSchemaProvider());
	}
}
