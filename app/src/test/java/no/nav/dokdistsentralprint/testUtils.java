package no.nav.dokdistsentralprint;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class testUtils {
	private testUtils() {
	}

	public static String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}

	public static String fileToString(File file) throws IOException {
		byte[] data = new byte[(int) file.length()];
		try (FileInputStream fis = new FileInputStream(file)) {
			fis.read(data);
		}
		return new String(data);
	}

	public static void unzipToDirectory(String zipFileName, Path outDir) throws IOException {
		byte[] buffer = new byte[2048];
		try (FileInputStream fis = new FileInputStream(zipFileName);
			 BufferedInputStream bis = new BufferedInputStream(fis);
			 ZipInputStream stream = new ZipInputStream(bis)) {
			ZipEntry entry;
			while ((entry = stream.getNextEntry()) != null) {
				Path filePath = outDir.resolve(entry.getName());
				try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
					 BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {
					int len;
					while ((len = stream.read(buffer)) > 0) {
						bos.write(buffer, 0, len);
					}
				}
			}
		}
	}
}
