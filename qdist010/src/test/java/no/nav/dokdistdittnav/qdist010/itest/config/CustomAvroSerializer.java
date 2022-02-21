package no.nav.dokdistdittnav.qdist010.itest.config;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

public class CustomAvroSerializer extends KafkaAvroSerializer {
	public CustomAvroSerializer() {
		super(SerializationUtils.REGISTRY);
	}
}
