package no.nav.dokdistdittnav.kafka;

import no.nav.tms.varsel.builder.InaktiverVarselBuilder;

public class InaktiverVarselMapper {

	public static String mapInaktiverVarsel(String varselId) {
		return InaktiverVarselBuilder.newInstance()
				.withVarselId(varselId)
				.build();
	}

}