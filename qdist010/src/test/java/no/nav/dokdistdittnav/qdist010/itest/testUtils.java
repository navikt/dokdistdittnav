package no.nav.dokdistdittnav.qdist010.itest;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class testUtils {
	private testUtils() {
	}

	public static String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}
}
