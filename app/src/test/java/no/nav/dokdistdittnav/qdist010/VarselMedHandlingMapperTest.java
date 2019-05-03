package no.nav.dokdistdittnav.qdist010;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import no.nav.dokdistdittnav.consumer.dokkat.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.qdist010.map.VarselMedHandlingMapper;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory;
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
	private XMLGregorianCalendar FERDIGSTILL_DATO;
	private static final String FORSENDELSE_TITTEL = "forsendelsertittel";

	private static VarselMedHandlingMapper varselMedHandlingMapper = new VarselMedHandlingMapper();

	@Test
	public void shouldMap() throws DatatypeConfigurationException {
		FERDIGSTILL_DATO = getXMLGregorianCalendarNow();
		VarselMedHandling varselMedHandling = varselMedHandlingMapper.map(createHentForsendelseResponseTo(),
				createDokumentTypeInfoTo(),
				VARSEL_BESTILLINGS_ID,
				FERDIGSTILL_DATO);

		assertEquals(VARSEL_BESTILLINGS_ID,varselMedHandling.getVarselbestillingId());
		assertEquals(createPerson().getIdent(), ((Person)varselMedHandling.getMottaker()).getIdent());
		assertEquals(VARSEL_TYPE_ID, varselMedHandling.getVarseltypeId());
		assertEquals(FORSENDELSE_TITTEL, varselMedHandling.getParameterListe().get(0).getValue());
		assertEquals(FERDIGSTILL_DATO.toString(), varselMedHandling.getParameterListe().get(1).getValue());
		assertEquals(VARSEL_BESTILLINGS_ID, varselMedHandling.getParameterListe().get(2).getValue());
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

	private XMLGregorianCalendar getXMLGregorianCalendarNow()
			throws DatatypeConfigurationException {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
		return datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
	}

	private Person createPerson(){
		ObjectFactory varselOF = new ObjectFactory();
		Person aktoer = varselOF.createPerson();
		aktoer.setIdent(MOTTAKER_ID);
		return aktoer;
	}

}
