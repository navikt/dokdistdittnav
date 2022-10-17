package no.nav.dokdistdittnav.consumer.doknotifikasjon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
@AllArgsConstructor
public class NotifikasjonInfoTo {
	int id;
	String bestillerId;
	String status;
	int antallRenotifikasjoner;
	Set<NotifikasjonDistribusjonDto> notifikasjonDistribusjoner;
}
