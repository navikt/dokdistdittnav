package no.nav.dokdistdittnav.kafka;

import lombok.SneakyThrows;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class FunctionalUtils {
	private FunctionalUtils() {
	}

	@SneakyThrows
	public static String classpathToString(String path) {
		try {
			InputStream inputStream = new ClassPathResource(path).getInputStream();
			String message = new String(inputStream.readAllBytes(), UTF_8);
			IOUtils.closeQuietly(inputStream);
			return message;
		} catch (IOException e) {
			throw new IOException(format("Kunne ikke åpne classpath-ressurs %s", path), e);
		}
	}
}
