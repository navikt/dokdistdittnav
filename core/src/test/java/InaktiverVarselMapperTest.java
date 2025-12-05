import no.nav.tms.varsel.builder.BuilderEnvironment;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static no.nav.dokdistdittnav.kafka.InaktiverVarselMapper.mapInaktiverVarsel;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class InaktiverVarselMapperTest {

	private static final String DITTNAV_BESTILLINGSID = "811c0c5d-e74c-491a-8b8c-d94075c822c3";
	private static final String INAKTIVER_EVENT_NAME = "inaktiver";

	@BeforeEach
	void setUp() {
		BuilderEnvironment.extend(Map.of(
				"NAIS_CLUSTER_NAME", "test-fss",
				"NAIS_NAMESPACE", "teamdokumenthandtering",
				"NAIS_APP_NAME", "dokdistdittnav")
		);
	}

	@Test
	void skalMappeInaktiverVarsel() {
		String inaktiverVarsel = mapInaktiverVarsel(DITTNAV_BESTILLINGSID);

		var jsonResponse = new JSONObject(inaktiverVarsel);
		assertThat(jsonResponse.getString("varselId")).isEqualTo(DITTNAV_BESTILLINGSID);
		assertThat(jsonResponse.getString("@event_name")).isEqualTo(INAKTIVER_EVENT_NAME);
	}

}