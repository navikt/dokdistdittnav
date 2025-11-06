package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static no.nav.tms.varsel.action.EksternKanal.EPOST;
import static no.nav.tms.varsel.action.Sensitivitet.Substantial;
import static no.nav.tms.varsel.action.Varseltype.Beskjed;

public class BeskjedMapper extends AbstractVarselMapper {

	public static final String AARSOPPGAVE_DOKUMENTTYPEID = "000053";

	public static final String ANNET_VARSELTEKST = "Du har fått en melding som gjelder %s. Les meldingen i dokumentarkivet.";

	public static final String ANNET_SMSVARSLINGSTEKST = "Hei! Du har fått en melding fra Nav. Logg inn på Nav for å lese den. Vennlig hilsen Nav";
	public static final String ANNET_EPOSTVARSLINGSTITTEL = "Melding fra Nav";
	public static final String ANNET_EPOSTVARSLINGSTEKST;

	public static final String AARSOPPGAVE_SMSVARSLINGSTEKST = "Hei! Du har fått en årsoppgave fra Nav. Logg inn på Nav for å lese den. Vennlig hilsen Nav";
	public static final String AARSOPPGAVE_EPOSTVARSLINGSTITTEL = "Årsoppgave fra Nav";
	public static final String AARSOPPGAVE_EPOSTVARSLINGSTEKST;

	static {
		ANNET_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/melding_epostvarseltekst.html");
		AARSOPPGAVE_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/aarsoppgave_epostvarseltekst.html");
	}

	public static String opprettBeskjed(HentForsendelseResponse forsendelse, String lenkeTilVarsel) {
		String dokumenttypeId = forsendelse.getDokumenter().getFirst().getDokumenttypeId();

		return OpprettVarselBuilder.newInstance()
				.withType(Beskjed)
				.withVarselId(forsendelse.getBestillingsId())
				.withIdent(forsendelse.getMottaker().getMottakerId())
				.withTekst(SPRAAKKODE_BOKMAAL, format(ANNET_VARSELTEKST, forsendelse.getForsendelseTittel()), true)
				.withLink(lenkeTilVarsel)
				.withSensitivitet(Substantial)
				.withAktivFremTil(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(SYNLIGEDAGER))
				.withEksternVarsling(OpprettVarselBuilder.eksternVarsling()
						.withPreferertKanal(EPOST)
						.withSmsVarslingstekst(mapSmsVarslingstekst(dokumenttypeId))
						.withEpostVarslingstittel(mapEpostVarslingstittel(dokumenttypeId))
						.withEpostVarslingstekst(mapEpostVarslingstekst(dokumenttypeId))
						.withKanBatches(false)
						.withUtsettSendingTil(null)
				)
				.build();
	}

	private static String mapSmsVarslingstekst(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgave(dokumenttypeId) ? AARSOPPGAVE_SMSVARSLINGSTEKST : ANNET_SMSVARSLINGSTEKST;
	}

	private static String mapEpostVarslingstittel(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgave(dokumenttypeId) ? AARSOPPGAVE_EPOSTVARSLINGSTITTEL : ANNET_EPOSTVARSLINGSTITTEL;
	}

	private static String mapEpostVarslingstekst(String dokumenttypeId) {
		return isDokumenttypeIdAarsoppgave(dokumenttypeId) ? AARSOPPGAVE_EPOSTVARSLINGSTEKST : ANNET_EPOSTVARSLINGSTEKST;
	}

	private static boolean isDokumenttypeIdAarsoppgave(String dokumenttypeId) {
		return AARSOPPGAVE_DOKUMENTTYPEID.equals(dokumenttypeId);
	}

}