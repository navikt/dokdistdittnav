package no.nav.dokdistdittnav.kdist002;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import no.nav.dokdistdittnav.consumer.doknotifikasjon.NotifikasjonInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponse;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {

	public static final String FORSENDELSE_ID = "1720847";
	public static final String DOKNOTIFIKASJON_BESTILLINGSID_OLD = "B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3";
	public static final String DOKNOTIFIKASJON_BESTILLINGSID_NEW = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	public static final String BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	public static final String MELDING = "Altinn feilet";
	public static final String DOKDISTDPI = "dokdistdpi";
	public static final String DOKDISTDITTNAV = "dokdistdittnav";

	public static HentForsendelseResponse hentForsendelseResponse() {
		String forsendelseString = classpathToString("__files/rdist001/hentForsendelseresponse-happy.json");
		return objectMapper(forsendelseString, HentForsendelseResponse.class);
	}

	public static HentForsendelseResponse hentForsendelseResponseUtenMottaker() {
		String forsendelseString = classpathToString("__files/rdist001/hentForsendelseresponse-uten-mottaker.json");
		return objectMapper(forsendelseString, HentForsendelseResponse.class);
	}

	public static NotifikasjonInfoTo hentNotifikasjonInfoTo() {
		String forsendelseString = classpathToString("__files/rnot001/doknot-happy.json");
		return objectMapper(forsendelseString, NotifikasjonInfoTo.class);
	}

	public static HentForsendelseResponse hentForsendelseResponseWithForsendelseStatusFeilet() {
		String forsendelseString = classpathToString("__files/rdist001/forsendelseresponse-with-forsendelsestatus-feil.json");
		return objectMapper(forsendelseString, HentForsendelseResponse.class);
	}

	public static FinnForsendelseResponse finnForsendelseResponseTo() {
		String forsendelseString = classpathToString("__files/rdist001/finnForsendelseresponse-happy.json");
		return objectMapper(forsendelseString, FinnForsendelseResponse.class);
	}

	@SneakyThrows
	private static <T> T objectMapper(String input, Class<T> tClass) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(WRITE_DATES_AS_TIMESTAMPS);
		return mapper.readValue(input, tClass);
	}

	@SneakyThrows
	public static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);

	}
}
