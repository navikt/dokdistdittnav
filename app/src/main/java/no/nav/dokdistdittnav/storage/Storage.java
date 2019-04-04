package no.nav.dokdistdittnav.storage;

import java.util.Optional;

public interface Storage {

	void put(String key, String value);

	Optional<String> get(String key);

	void delete(String key);
}
