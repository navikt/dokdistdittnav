package no.nav.dokdistdittnav.consumer.rdist001.kodeverk;

public enum Oppslagsnoekkel {

	KONVERSASJONSID("konversasjonsId"),
	BESTILLINGSID("bestillingsId"),
	JOURNALPOSTID("journalpostId");

	public final String noekkel;

	Oppslagsnoekkel(String noekkel) {
		this.noekkel = noekkel;
	}
}