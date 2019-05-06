package no.nav.dokdistdittnav.qdist010;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import no.nav.dokdistdittnav.consumer.dokkat.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.technical.KunneIkkeHenteDagensDatoTechnicalException;
import no.nav.dokdistdittnav.qdist010.map.VarselMedHandlingMapper;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Person;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.junit.jupiter.api.Test;

import java.util.GregorianCalendar;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
public class VarselMedHandlingMapperTest {

	private static final String MOTTAKER_ID = "mottakerId";
	private static final String VARSEL_TYPE_ID = "varseltypeId";
	private static final String VARSEL_BESTILLINGS_ID = "varselbestillingsId";
	private final XMLGregorianCalendar FERDIGSTILL_DATO = getXMLGregorianCalendarNow();
	private static final String FORSENDELSE_TITTEL = "forsendelsertittel";
	private static final String DOKUMENTTITTEL_KEY = "DokumentTittel";
	private static final String FERDIGSTILTDATO_KEY = "FerdigstiltDato";
	private static final String VARSELBESTILLING_ID_KEY = "VarselbestillingsId";

	private static VarselMedHandlingMapper varselMedHandlingMapper = new VarselMedHandlingMapper();

	@Test
	public void shouldMap() {
		VarselMedHandling varselMedHandling = varselMedHandlingMapper.map(createHentForsendelseResponseTo(),
				createDokumentTypeInfoTo(),
				VARSEL_BESTILLINGS_ID,
				FERDIGSTILL_DATO);

		assertEquals(VARSEL_BESTILLINGS_ID, varselMedHandling.getVarselbestillingId());
		assertEquals(MOTTAKER_ID, ((Person) varselMedHandling.getMottaker()).getIdent());
		assertEquals(VARSEL_TYPE_ID, varselMedHandling.getVarseltypeId());
		assertEquals(FORSENDELSE_TITTEL, varselMedHandling.getParameterListe().get(0).getValue());
		assertEquals(FERDIGSTILL_DATO.toString(), varselMedHandling.getParameterListe().get(1).getValue());
		assertEquals(VARSEL_BESTILLINGS_ID, varselMedHandling.getParameterListe().get(2).getValue());
		assertEquals(DOKUMENTTITTEL_KEY, varselMedHandling.getParameterListe().get(0).getKey());
		assertEquals(FERDIGSTILTDATO_KEY, varselMedHandling.getParameterListe().get(1).getKey());
		assertEquals(VARSELBESTILLING_ID_KEY, varselMedHandling.getParameterListe().get(2).getKey());
		assertEquals(null, varselMedHandling.getUtloepstidspunkt());
	}

	private HentForsendelseResponseTo createHentForsendelseResponseTo() {
		return HentForsendelseResponseTo.builder()
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.mottaker(HentForsendelseResponseTo.MottakerTo.builder()
						.mottakerId(MOTTAKER_ID)
						.build())
				.build();
	}

	private DokumenttypeInfoTo createDokumentTypeInfoTo() {
		return DokumenttypeInfoTo.builder()
				.varselTypeId(VARSEL_TYPE_ID)
				.build();
	}

	private XMLGregorianCalendar getXMLGregorianCalendarNow() {
		XMLGregorianCalendar now;
		try {
			now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
		} catch (DatatypeConfigurationException e) {
			throw new KunneIkkeHenteDagensDatoTechnicalException("QDIST010 kunne ikke hente dagens dato", e);
		}
		return now;
	}
}
