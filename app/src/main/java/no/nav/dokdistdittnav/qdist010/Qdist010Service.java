package no.nav.dokdistdittnav.qdist010;

import static no.nav.dokdistdittnav.qdist010.Qdist010Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdittnav.qdist010.util.Qdist010FunctionalUtils.getDokumenttypeIdHoveddokument;
import static no.nav.dokdistdittnav.qdist010.util.Qdist010FunctionalUtils.validateForsendelseStatus;

import no.nav.dokdistdittnav.consumer.dokkat.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistdittnav.consumer.dokkat.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistdittnav.consumer.dokkat.tkat021.VarselInfo;
import no.nav.dokdistdittnav.consumer.dokkat.tkat021.VarselInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistdittnav.exception.technical.KunneIkkeHenteDagensDatoTechnicalException;
import no.nav.dokdistdittnav.qdist010.domain.DistribuerForsendelseTilDittNavTo;
import no.nav.dokdistdittnav.qdist010.map.DokumenthenvendelseMapper;
import no.nav.dokdistdittnav.qdist010.map.VarselMedHandlingMapper;
import no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.Dokumenthenvendelse;
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

	public static final String PROPERTY_UNMARSHALLED_VARSEL = "QDIST010.varsel";

	private final DokumenthenvendelseMapper dokumenthenvendelseMapper = new DokumenthenvendelseMapper();
	private final VarselMedHandlingMapper varselMedHandlingMapper = new VarselMedHandlingMapper();

	@Inject
	public Qdist010Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   VarselInfo varselInfo,
						   AdministrerForsendelse administrerForsendelse) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.varselInfo = varselInfo;
		this.administrerForsendelse = administrerForsendelse;
	}

	@Handler
	public Dokumenthenvendelse distribuerForsendelseTilDittNAVService(DistribuerForsendelseTilDittNavTo distribuerForsendelseTilDittNavTo, Exchange exchange) {
		HentForsendelseResponseTo hentForsendelseResponseTo = administrerForsendelse.hentForsendelse(distribuerForsendelseTilDittNavTo
				.getForsendelseId());
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponseTo.getBestillingsId());
		validateForsendelseStatus(hentForsendelseResponseTo.getForsendelseStatus());
		DokumenttypeInfoTo dokumenttypeInfoTo = dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(hentForsendelseResponseTo));

		String varselbestillingId = UUID.randomUUID().toString();
		XMLGregorianCalendar now = getNow();
		VarselInfoTo varselInfoTo = varselInfo.getVarselInfo(dokumenttypeInfoTo.getVarselTypeId());
		VarselMedHandling varsel = varselMedHandlingMapper.map(hentForsendelseResponseTo, dokumenttypeInfoTo, varselbestillingId, now);
		exchange.setProperty(PROPERTY_UNMARSHALLED_VARSEL, varsel);

		return dokumenthenvendelseMapper.map(hentForsendelseResponseTo, varselInfoTo, varselbestillingId, now);
	}

	private XMLGregorianCalendar getNow() {
		XMLGregorianCalendar now;
		try {
			now = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
		} catch (DatatypeConfigurationException e) {
			throw new KunneIkkeHenteDagensDatoTechnicalException("qdist010 kunne ikke hente dagens dato", e);
		}
		return now;
	}

}
