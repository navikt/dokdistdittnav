package no.nav.dokdistdittnav.qdist010.map;

import no.nav.dokdistdittnav.consumer.dokkat.tkat021.VarselInfoTo;
import no.nav.dokdistdittnav.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.Arkivtemaer;
import no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.Dokumenthenvendelse;
import no.nav.melding.virksomhet.opprettdokumenthenvendelse.v1.opprettdokumenthenvendelse.ObjectFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.stream.Collectors;

/**
 * @author Erik Bråten, Visma Consulting.
 */
public class DokumenthenvendelseMapper {

    public Dokumenthenvendelse map(HentForsendelseResponseTo forsendelse, VarselInfoTo varselInfo, String varselbestillingId, XMLGregorianCalendar now) {
        ObjectFactory henvendelseOF = new ObjectFactory();
        Dokumenthenvendelse dokumenthenvendelse = henvendelseOF.createDokumenthenvendelse();
        dokumenthenvendelse.setPersonident(forsendelse.getMottaker().getMottakerId());
        dokumenthenvendelse.setDokumenttittel(forsendelse.getForsendelseTittel());
        Arkivtemaer arkivtemaer = henvendelseOF.createArkivtemaer();
        arkivtemaer.setValue(forsendelse.getTema());
        dokumenthenvendelse.setArkivtema(arkivtemaer);
        dokumenthenvendelse.setFerdigstiltDato(now);
        dokumenthenvendelse.setJournalpostId(forsendelse.getArkivInformasjon().getArkivId());
        dokumenthenvendelse.getDokumentIdListe().addAll(forsendelse.getDokumenter().stream()
                .map(HentForsendelseResponseTo.DokumentTo::getArkivDokumentInfoId)
                .collect(Collectors.toList()));
        dokumenthenvendelse.setVarselbestillingId(varselbestillingId);
        dokumenthenvendelse.setStoppRepeterendeVarsel(varselInfo.isStoppRepeterendeVarsel());

        return dokumenthenvendelse;
    }
}
