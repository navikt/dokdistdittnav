package no.nav.dokdistdittnav.qdist010.map;

import no.nav.dokdistdittnav.consumer.dokkat.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.ObjectFactory;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Parameter;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.Person;
import no.nav.melding.virksomhet.varselmedhandling.v1.varselmedhandling.VarselMedHandling;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Erik Bråten, Visma Consulting.
 */
public class VarselMedHandlingMapper {

    private static final String DOKUMENTTITTEL_KEY = "DokumentTittel";
    private static final String FERDIGSTILTDATO_KEY = "FerdigstiltDato";
    private static final String VARSELBESTILLING_ID_KEY = "VarselbestillingsId";

    public VarselMedHandling map(HentForsendelseResponseTo forsendelse, DokumenttypeInfoTo dokumenttypeInfo, String varselbestillingId, XMLGregorianCalendar now) {
        ObjectFactory varselOF = new ObjectFactory();
        VarselMedHandling varselMedHandling = varselOF.createVarselMedHandling();
        varselMedHandling.setVarselbestillingId(varselbestillingId);
        varselMedHandling.setReVarsel(false);
        varselMedHandling.setMottaker(getMottaker(forsendelse.getMottaker().getMottakerId(), varselOF));
        varselMedHandling.setVarseltypeId(dokumenttypeInfo.getVarselTypeId());
        varselMedHandling.getParameterListe().add(createParameter(varselOF, DOKUMENTTITTEL_KEY, forsendelse.getForsendelseTittel()));
        varselMedHandling.getParameterListe().add(createParameter(varselOF, FERDIGSTILTDATO_KEY, now != null ? now.toString() : ""));
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
}
