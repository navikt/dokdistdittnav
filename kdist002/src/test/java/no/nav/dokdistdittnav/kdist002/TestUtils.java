package no.nav.dokdistdittnav.kdist002;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import no.nav.dokdistdittnav.config.properties.DokdistdittnavProperties;
import no.nav.dokdistdittnav.consumer.doknotifikasjon.NotifikasjonInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.FinnForsendelseResponseTo;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponseTo;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {

	public static final String FORSENDELSE_ID = "1720847";
	public static final String NY_FORSENDELSE_ID = "33333";
	public static final String DOKNOTIFIKASJON_BESTILLINGSID_OLD = "B-dokdistdittnav-811c0c5d-e74c-491a-8b8c-d94075c822c3";
	public static final String DOKNOTIFIKASJON_BESTILLINGSID_NEW = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	public static final String DOKNOTIFIKASJON_BESTILLINGSID_FEIL_UUID = "O-dokdistdittnav-9f7dac2c-0c00-3a0d-aab9-1a51449b1d1a";
	public static final String BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	public static final String MELDING = "Altinn feilet";
	public static final String DOKDISTDPI = "dokdistdpi";
	public static final String DOKDISTDITTNAV = "dokdistdittnav";

	public static DokdistdittnavProperties dokdistdittnavProperties() {
		DokdistdittnavProperties dokdistdittnavProperties = new DokdistdittnavProperties();
		dokdistdittnavProperties.setAppnavn(DOKDISTDITTNAV);
		return dokdistdittnavProperties;
	}

	public static HentForsendelseResponseTo hentForsendelseResponseTo() {
		String forsendelseString = classpathToString("__files/rdist001/hentForsendelseresponse-happy.json");
		return objectMapper(forsendelseString, HentForsendelseResponseTo.class);
	}

	public static NotifikasjonInfoTo hentNotifikasjonInfoTo() {
		String forsendelseString = classpathToString("__files/rnot001/doknot-happy.json");
		return objectMapper(forsendelseString, NotifikasjonInfoTo.class);
	}

	public static HentForsendelseResponseTo hentForsendelseResponseWithForsendelseStatusFeilet() {
		String forsendelseString = classpathToString("__files/rdist001/forsendelseresponse-with-forsendelsestatus-feil.json");
		return objectMapper(forsendelseString, HentForsendelseResponseTo.class);
	}

	public static FinnForsendelseResponseTo finnForsendelseResponseTo() {
		String forsendelseString = classpathToString("__files/rdist001/finnForsendelseresponse-happy.json");
		return objectMapper(forsendelseString, FinnForsendelseResponseTo.class);
	}


	//@SneakyThrows
	private static <T> T objectMapper(String input, Class<T> tClass) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		try {
			return mapper.readValue(input, tClass);
		} catch (Exception e){
			System.out.println("hecc");
		}
		return null;
	}

	@SneakyThrows
	public static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);

	}
}
