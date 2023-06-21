package no.nav.dokdistdittnav.kdist002.mapper;

import no.nav.dokdistdittnav.consumer.doknotifikasjon.NotifikasjonInfoTo;
import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.Notifikasjon;
import no.nav.dokdistdittnav.consumer.dokumentdistribusjon.OppdaterVarselInfoRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.nav.dokdistdittnav.kdist002.mapper.OppdaterVarselInfoMapper.mapNotifikasjonBestilling;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class OppdaterVarselInfoMapperTest {

	private static final String bestillingsId = "dette_er_en_bestillingsId";
	private static final String EPOST = "EPOST";
	private static final String IKKE_EPOST = "NOE_ANNET";
	private static final String TITTEL = "Dette er en tittel";
	private static final String KONTAKTINFO = "kontaktInfo";
	private static final String BESTILLERID = "DITTNAV";
	private static final String STATUS = "OVERSENDT";
	private static final String TEKST = "Dette er en tekst";
	private static final String MOBILTELEFON = "MOBILTELEFON";
	private static final LocalDateTime NOW = LocalDateTime.now();

	@Test
	public void shouldMap() {
		Set<NotifikasjonInfoTo.NotifikasjonDistribusjonDto> distribusjoner = new HashSet<>();
		distribusjoner.add(createNotifikasjonDistribusjonDto(EPOST));
		distribusjoner.add(createNotifikasjonDistribusjonDto(IKKE_EPOST));
		NotifikasjonInfoTo notifikasjonInfoTo = createNotifikasjonInfoTo(distribusjoner);

		OppdaterVarselInfoRequest oppdaterVarselInfoRequest = mapNotifikasjonBestilling(bestillingsId, notifikasjonInfoTo);

		assertThat(oppdaterVarselInfoRequest.forsendelseId(), is(bestillingsId));
		assertThat(oppdaterVarselInfoRequest.notifikasjoner().size(), is(2));
		List<Notifikasjon> notifikasjoner = oppdaterVarselInfoRequest.notifikasjoner().stream()
				.sorted(Comparator.comparing(Notifikasjon::kanal))
				.toList();
		assertNotifikasjonInfoTo(notifikasjoner.get(0), EPOST, TITTEL);
		assertNotifikasjonInfoTo(notifikasjoner.get(1), MOBILTELEFON, null);

	}

	private void assertNotifikasjonInfoTo(Notifikasjon notifikasjon, String kanal, String tittel) {
		assertThat(notifikasjon.kanal(), is(kanal));
		assertThat(notifikasjon.tittel(), is(tittel));
		assertThat(notifikasjon.kontaktInfo(), is(KONTAKTINFO));
		assertThat(notifikasjon.tekst(), is(TEKST));
		assertThat(notifikasjon.varslingstidspunkt(), is(NOW));
	}

	public NotifikasjonInfoTo createNotifikasjonInfoTo(Set<NotifikasjonInfoTo.NotifikasjonDistribusjonDto> distribusjoner) {
		return new NotifikasjonInfoTo(1, BESTILLERID, STATUS, 0, distribusjoner);
	}

	public NotifikasjonInfoTo.NotifikasjonDistribusjonDto createNotifikasjonDistribusjonDto(String kanal) {
		return new NotifikasjonInfoTo.NotifikasjonDistribusjonDto(1, STATUS, kanal, KONTAKTINFO, TITTEL, TEKST, NOW);
	}
}