package no.nav.dokdistsentralprint.storage;

import static no.nav.dokdistdittnav.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistdittnav.storage.config.StorageConfiguration.BUCKET_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import no.nav.dokdistdittnav.exception.technical.KunneIkkeLeseFraS3BucketTechnicalException;
import no.nav.dokdistdittnav.storage.DokdistDokument;
import no.nav.dokdistdittnav.storage.JsonSerializer;
import no.nav.dokdistdittnav.storage.S3Storage;
import no.nav.dokdistdittnav.storage.Storage;
import no.nav.dokdistdittnav.storage.crypto.Crypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@ActiveProfiles("unittest")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = S3StorageTest.Config.class)
public class S3StorageTest {

	private final byte[] pdf = "PDF test document".getBytes();
	private static final String encryptPsw = "psw";
	private final String key = "test_key-asdsdasdsad";

	@Inject
	private AmazonS3 s3;

	@Inject
	private Storage storage;

	@BeforeEach
	public void setUp() {
		reset(s3);
	}


	@Test
	public void shouldRetryGetWhenFailed() {
		when(s3.getObject(any(String.class), any(String.class))).thenThrow(new KunneIkkeLeseFraS3BucketTechnicalException("asd"));

		try {
			storage.get(key);
		} catch (Exception e) {
			verify(s3, times(MAX_ATTEMPTS_SHORT)).getObject(any(String.class), any(String.class));
		}
	}

	@Test
	public void shouldGetObjectAndDecrypt() {
		when(s3.getObject(any(String.class), any(String.class))).thenReturn(createEncryptedS3Object());
		String result = storage.get(key).get();

		verify(s3).getObject(BUCKET_NAME, key);
		assertThat(result, equalTo(JsonSerializer.serialize(createDokument())));
	}


	private S3Object createEncryptedS3Object() {
		S3Object s3Object = new S3Object();
		s3Object.setObjectContent(new ByteArrayInputStream(new Crypto(encryptPsw, key).encrypt(JsonSerializer.serialize(createDokument()))
				.getBytes()));
		return s3Object;
	}

	private DokdistDokument createDokument() {
		return DokdistDokument.builder()
				.pdf(pdf)
				.build();
	}

	@Profile("unittest")
	@EnableRetry
	@Configuration
	public static class Config {
		@Bean
		static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			PropertySourcesPlaceholderConfigurer placeholder = new PropertySourcesPlaceholderConfigurer();
			placeholder.setIgnoreUnresolvablePlaceholders(true);

			return placeholder;
		}

		@Bean
		public AmazonS3 s3() {
			return mock(AmazonS3.class);
		}

		@Bean
		public Storage storage(AmazonS3 s3) {
			return new S3Storage(s3, encryptPsw);
		}

	}

}