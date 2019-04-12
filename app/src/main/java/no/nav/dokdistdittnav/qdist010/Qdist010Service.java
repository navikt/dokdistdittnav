package no.nav.dokdistdittnav.qdist010;

import static no.nav.dokdistdittnav.qdist010.util.Qdist010FunctionalUtils.getDokumenttypeIdHoveddokument;
import static no.nav.dokdistdittnav.qdist010.util.Qdist010FunctionalUtils.validateForsendelseStatus;

import no.nav.dokdistdittnav.consumer.dokkat.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistdittnav.consumer.dokkat.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistdittnav.consumer.dokkat.tkat021.VarselInfo;
import no.nav.dokdistdittnav.consumer.dokkat.tkat021.VarselInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.metrics.MetricUpdater;
import no.nav.dokdistdittnav.qdist010.domain.DistribuerForsendelseTilSentralPrintTo;
import no.nav.dokdistdittnav.storage.Storage;
import no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.Dokumenthenvendelse;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Person;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.UUID;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist010Service {

	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final VarselInfo varselInfo;
	private final AdministrerForsendelse administrerForsendelse;
	private final Storage storage;
	private final MetricUpdater metricUpdater;

	public static final String PROPERTY_UNMARSHALLED_VARSEL = "QDIST010.varsel";
	private static final String DOKUMENTTITTEL_KEY = "DokumentTittel";
	private static final String FERDIGSTILTDATO_KEY = "FerdigstiltDato";
	private static final String VARSELBESTILLING_ID_KEY = "VarselbestillingsId";

	@Inject
	public Qdist010Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   VarselInfo varselInfo,
						   AdministrerForsendelse administrerForsendelse,
						   Storage storage,
						   MetricUpdater metricUpdater) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.varselInfo = varselInfo;
		this.administrerForsendelse = administrerForsendelse;
		this.storage = storage;
		this.metricUpdater = metricUpdater;
	}

	@Handler
	public Dokumenthenvendelse distribuerForsendelseTilDittNAVService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo, Exchange exchange) {
		HentForsendelseResponseTo hentForsendelseResponseTo = administrerForsendelse.hentForsendelse(distribuerForsendelseTilSentralPrintTo
				.getForsendelseId());
		validateForsendelseStatus(hentForsendelseResponseTo.getForsendelseStatus());
		DokumenttypeInfoTo dokumenttypeInfoTo = dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(hentForsendelseResponseTo));

		String varselbestillingId = UUID.randomUUID().toString();
		VarselInfoTo varselInfoTo = varselInfo.getVarselInfo(dokumenttypeInfoTo.getVarselTypeId());
		VarselMedHandling varsel = mapToVarselMedHandling(hentForsendelseResponseTo, varselInfoTo, dokumenttypeInfoTo, varselbestillingId);
		exchange.setProperty(PROPERTY_UNMARSHALLED_VARSEL, varsel);

		return mapToDokumenthenvendelse(hentForsendelseResponseTo, varselInfoTo, varselbestillingId);
	}

	private Dokumenthenvendelse mapToDokumenthenvendelse(HentForsendelseResponseTo forsendelse, VarselInfoTo varselInfo, String varselbestillingId) {
		no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.ObjectFactory henvendelseOF =
				new no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.ObjectFactory();
		Dokumenthenvendelse dokumenthenvendelse = henvendelseOF.createDokumenthenvendelse();
		dokumenthenvendelse.setPersonident(forsendelse.getMottaker().getMottakerId());
		//dokumenthenvendelse.setDokumenttittel(forsendelse.getForsendelseTittel());
		//dokumenthenvendelse.setArkivtema(forsendelse.getTema());
		//dokumenthenvendelse.setFerdigstiltDato(forsendelse.?);
		//dokumenthenvendelse.setJournalpostId(forsendelse.getArkivInformasjon().getArkivId());
		//dokumenthenvendelse.getDokumentIdListe().addAll(forsendelse.getDokumenter()...);
		dokumenthenvendelse.setVarselbestillingId(varselbestillingId);
		dokumenthenvendelse.setStoppRepeterendeVarsel(varselInfo.isStoppRepeterendeVarsel());

		return dokumenthenvendelse;
    }

    private VarselMedHandling mapToVarselMedHandling(HentForsendelseResponseTo forsendelse, VarselInfoTo varselInfo, DokumenttypeInfoTo dokumenttypeInfo, String varselbestillingId) {
		ObjectFactory varselOF = new ObjectFactory();
		VarselMedHandling varselMedHandling = varselOF.createVarselMedHandling();
		varselMedHandling.setVarselbestillingId(varselbestillingId);
		varselMedHandling.setReVarsel(false);
		varselMedHandling.setMottaker(getMottaker(forsendelse.getMottaker().getMottakerId(), varselOF));
		varselMedHandling.setVarseltypeId(dokumenttypeInfo.getVarselTypeId());
		//varselMedHandling.getParameterListe().add(createParameter(varselOF, DOKUMENTTITTEL_KEY, forsendelse.getForsendelseTittel()));
		varselMedHandling.getParameterListe().add(createParameter(varselOF, FERDIGSTILTDATO_KEY, getNow()));
		varselMedHandling.getParameterListe().add(createParameter(varselOF, VARSELBESTILLING_ID_KEY, varselbestillingId));

		return varselMedHandling;
	}

	private Person getMottaker(String mottakerId, ObjectFactory varselOF) {
		Person aktoer = varselOF.createPerson();
		aktoer.setIdent(mottakerId);
		return aktoer;
	}

	private Parameter createParameter(ObjectFactory varselOF, String key, String value) {
		Parameter param = varselOF.createParameter();
		param.setKey(key);
		param.setValue(value);
		return param;
	}

	private String getNow() {
		XMLGregorianCalendar now = null;
		try {
			now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
		} catch (DatatypeConfigurationException e) {
			// Ignore
		}
		return now != null ? now.toString() : "";
	}

}
