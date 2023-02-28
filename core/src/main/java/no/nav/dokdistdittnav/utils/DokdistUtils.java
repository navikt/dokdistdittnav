package no.nav.dokdistdittnav.utils;

import lombok.SneakyThrows;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class DokdistUtils {
	private DokdistUtils() {
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

	public static void assertNotNull(String feltnavn, Object obj) {
		if (isNull(obj)) {
			throw new IllegalArgumentException(format("%s kan ikke være null", feltnavn));
		}
	}

	public static void assertNotBlank(String feltnavn, String value) {
		if (isBlank(value)) {
			throw new IllegalArgumentException(format("%s kan ikke være null", feltnavn));
		}
	}
}
