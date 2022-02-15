package no.nav.dokdistdittnav.itest.config;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;

public class SerializationUtils {
	static final SchemaRegistryClient REGISTRY = new MockSchemaRegistryClient();

	private SerializationUtils() {
	}
}
