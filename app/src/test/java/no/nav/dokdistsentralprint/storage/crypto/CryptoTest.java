package no.nav.dokdistsentralprint.storage.crypto;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import no.nav.dokdistdittnav.storage.DokdistDokument;
import no.nav.dokdistdittnav.storage.JsonSerializer;
import no.nav.dokdistdittnav.storage.crypto.Crypto;
import org.junit.jupiter.api.Test;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
public class CryptoTest {

	@Test
	public void shouldEncryptAndDecrypt() {
		Crypto crypto = new Crypto("wDPCTP3kAFYcjupjcR4ETVOK6Ak8+c3TZBOHkj2inNw=", "123213213");
		String jsonDocument = JsonSerializer.serialize(createDokument());
		String encrypted = crypto.encrypt(jsonDocument);

		assertNotEquals(jsonDocument, encrypted);
		String decrypted = crypto.decrypt(encrypted);

		assertThat(decrypted, is(equalTo(jsonDocument)));
	}

	private DokdistDokument createDokument() {
		return DokdistDokument.builder()
				.pdf(new byte[1000000]).build();
	}

}