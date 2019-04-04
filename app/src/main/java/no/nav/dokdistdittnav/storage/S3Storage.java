package no.nav.dokdistdittnav.storage;

import static java.util.stream.Collectors.joining;
import static no.nav.dokdistdittnav.storage.config.StorageConfiguration.BUCKET_NAME;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdittnav.constants.RetryConstants;
import no.nav.dokdistdittnav.exception.technical.AbstractDokdistdittnavTechnicalException;
import no.nav.dokdistdittnav.exception.technical.KunneIkkeLeseFraS3BucketTechnicalException;
import no.nav.dokdistdittnav.storage.crypto.Crypto;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;

@Slf4j
public class S3Storage implements Storage {

	private AmazonS3 s3;
	private String encryptionPassphrase;

	@Inject
	public S3Storage(AmazonS3 s3, String encryptionPassphrase) {
		this.s3 = s3;
		this.encryptionPassphrase = encryptionPassphrase;
	}

	@Override
	public void put(String key, String value) {
		throw new UnsupportedOperationException("dokdistdittnav støtter ikke persistering av objekter til dokdistmellomlager");
	}

	@Override
	@Retryable(include = AbstractDokdistdittnavTechnicalException.class, backoff = @Backoff(delay = RetryConstants.DELAY_SHORT, multiplier = RetryConstants.MULTIPLIER_SHORT))
	public Optional<String> get(String key) {
		try {
			String encryptedValue = readString(key);
			if (encryptedValue == null) {
				return Optional.empty();
			}
			return Optional.ofNullable(decrypt(encryptedValue, key));
		} catch (Exception e) {
			throw new KunneIkkeLeseFraS3BucketTechnicalException(String.format("Feilet ved henting av dokument fra S3-bucketen dokdistmellomlager. Nøkkel=%s", key), e);
		}
	}

	@Override
	public void delete(String key) {
		throw new UnsupportedOperationException("dokdistdittnav støtter ikke sletting av objekter fra dokdistmellomlager");
	}

	private String readString(String key) {
		S3Object object;
		try {
			object = s3.getObject(BUCKET_NAME, key);
		} catch (AmazonS3Exception ex) {
			log.warn("Kunne ikke hente objekt fra dokdistmellomlager med nøkkel={}. Årsaken er sanssynligvis at objektet ikke finnes.");
			return null;
		}

		return new BufferedReader(new InputStreamReader(object.getObjectContent()))
				.lines()
				.collect(joining("\n"));
	}

	private String decrypt(String encrypted, String key) {
		return new Crypto(encryptionPassphrase, key).decrypt(encrypted);
	}

}
