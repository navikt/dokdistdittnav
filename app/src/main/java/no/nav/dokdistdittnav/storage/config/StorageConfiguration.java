package no.nav.dokdistdittnav.storage.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import no.nav.dokdistdittnav.storage.S3Storage;
import no.nav.dokdistdittnav.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("nais")
public class StorageConfiguration {

	@Value("${dokdistdittnav_s3_creds_username}")
	private String accessKey;

	@Value("${dokdistdittnav_s3_creds_password}")
	private String secretKey;

	@Value("${storage_s3_url}")
	private String s3Endpoint;

	@Value("${dokdistmellomlager_s3_storage_crypto_password}")
	private String encryptionPassphrase;

	private static final String REGION_TO_USE_FOR_S3_TO_WORK_ONPREM = "us-east-1";
	public static final String BUCKET_NAME = "dokdistmellomlager";

	@Bean
	@Lazy
	public Storage storage() {
		AmazonS3 s3 = initS3Client();
		return new S3Storage(s3, encryptionPassphrase);
	}

	private AmazonS3 initS3Client() {
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

		return AmazonS3ClientBuilder.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, REGION_TO_USE_FOR_S3_TO_WORK_ONPREM))
				.enablePathStyleAccess()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
	}
}
