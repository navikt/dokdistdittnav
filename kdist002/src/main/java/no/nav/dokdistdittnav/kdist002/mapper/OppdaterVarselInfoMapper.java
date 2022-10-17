package no.nav.dokdistdittnav.kdist002.mapper;

import no.nav.dokdistdittnav.consumer.doknotifikasjon.NotifikasjonDistribusjonDto;
import no.nav.dokdistdittnav.consumer.doknotifikasjon.NotifikasjonInfoTo;
import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.Notifikasjon;
import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.OppdaterVarselInfoRequest;

import java.util.Set;
import java.util.stream.Collectors;

public class OppdaterVarselInfoMapper {

	private static final String EPOST = "EPOST";
	private static final String MOBILTELEFON = "MOBILTELEFON";

	public static OppdaterVarselInfoRequest mapNotifikasjonBestilling(String bestillingsId, NotifikasjonInfoTo notifikasjonInfoTo){
		Set<Notifikasjon> notifikasjoner = notifikasjonInfoTo.getNotifikasjonDistribusjoner().stream().map(distribusjon -> mapNotifikasjon(distribusjon)).collect(Collectors.toSet());
		return new OppdaterVarselInfoRequest(bestillingsId, notifikasjoner);
	}

	private static Notifikasjon mapNotifikasjon(NotifikasjonDistribusjonDto notifikasjonDist){
		String kanal = notifikasjonDist.getKanal();
		return new Notifikasjon(
				mapKanal(kanal),
				mapTittel(kanal, notifikasjonDist.getTittel()),
				notifikasjonDist.getTekst(),
				notifikasjonDist.getKontaktInfo());
	}

	private static String mapTittel(String kanal, String tittel){
		return EPOST.equals(kanal) ? tittel : null;
	}
	private static String mapKanal(String kanal) { return EPOST.equals(kanal) ? EPOST : MOBILTELEFON; }
}
