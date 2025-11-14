package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.FeilDistribusjonstypeForVarselTilMinSideException;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static no.nav.tms.varsel.action.EksternKanal.EPOST;
import static no.nav.tms.varsel.action.Sensitivitet.Substantial;
import static no.nav.tms.varsel.action.Varseltype.Oppgave;

public class OppgaveMapper extends AbstractVarselMapper {

	public static final String VEDTAK_VARSELTEKST = "Du har fått et vedtak som gjelder %s. Les vedtaket i dokumentarkivet. Hvis du ikke åpner og leser dokumentet, vil vi sende det til deg i posten.";
	public static final String VEDTAK_SMSVARSLINGSTEKST = "Hei! Du har fått et vedtak fra Nav. Logg inn på Nav for å lese det. Vennlig hilsen Nav";
	public static final String VEDTAK_EPOSTVARSLINGSTITTEL = "Vedtak fra Nav";
	public static final String VEDTAK_EPOSTVARSLINGSTEKST;

	public static final String VIKTIG_ELLER_NULL_VARSELTEKST = "Du har fått et brev som du må lese: %s. Les brevet i dokumentarkivet. Hvis du ikke åpner og leser dokumentet, vil vi sende det til deg i posten.";
	public static final String VIKTIG_ELLER_NULL_SMSVARSLINGSTEKST = "Hei! Du har fått et brev fra Nav som du må lese. Logg inn på Nav for å lese det. Vennlig hilsen Nav";
	public static final String VIKTIG_ELLER_NULL_EPOSTVARSLINGSTITTEL = "Brev fra Nav";
	public static final String VIKTIG_ELLER_NULL_EPOSTVARSLINGSTEKST;

	static {
		VEDTAK_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/vedtak_epostvarseltekst.html");
		VIKTIG_ELLER_NULL_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/viktig_epostvarseltekst.html");
	}

	public static String opprettOppgave(HentForsendelseResponse forsendelse, String lenkeTilVarsel) {
		if (!forsendelse.erDistribusjonstypeVedtakViktigEllerNull()) {
			throw new FeilDistribusjonstypeForVarselTilMinSideException("Sender kun varsel med type=oppgave til Min Side for distribusjonstype VEDTAK, VIKTIG, eller null. Mottok=%s".formatted(forsendelse.getDistribusjonstype()));
		}

		return OpprettVarselBuilder.newInstance()
				.withType(Oppgave)
				.withVarselId(forsendelse.getBestillingsId())
				.withIdent(forsendelse.getMottaker().getMottakerId())
				.withTekst(SPRAAKKODE_BOKMAAL, format(mapTekst(forsendelse), forsendelse.getForsendelseTittel()), true)
				.withLink(lagLenkeTilVarsel(lenkeTilVarsel, forsendelse))
				.withSensitivitet(Substantial)
				.withAktivFremTil(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(SYNLIGEDAGER))
				.withEksternVarsling(OpprettVarselBuilder.eksternVarsling()
						.withPreferertKanal(EPOST)
						.withSmsVarslingstekst(mapSmsVarslingstekst(forsendelse))
						.withEpostVarslingstittel(mapEpostVarslingstittel(forsendelse))
						.withEpostVarslingstekst(mapEpostVarslingstekst(forsendelse))
						.withKanBatches(false)
						.withUtsettSendingTil(null))
				.build();
	}

	private static String mapTekst(HentForsendelseResponse forsendelse) {
		return forsendelse.erDistribusjonstypeVedtak() ? VEDTAK_VARSELTEKST : VIKTIG_ELLER_NULL_VARSELTEKST;
	}

	private static String mapSmsVarslingstekst(HentForsendelseResponse forsendelse) {
		return forsendelse.erDistribusjonstypeVedtak() ? VEDTAK_SMSVARSLINGSTEKST : VIKTIG_ELLER_NULL_SMSVARSLINGSTEKST;
	}

	private static String mapEpostVarslingstittel(HentForsendelseResponse forsendelse) {
		return forsendelse.erDistribusjonstypeVedtak() ? VEDTAK_EPOSTVARSLINGSTITTEL : VIKTIG_ELLER_NULL_EPOSTVARSLINGSTITTEL;
	}

	private static String mapEpostVarslingstekst(HentForsendelseResponse forsendelse) {
		return forsendelse.erDistribusjonstypeVedtak() ? VEDTAK_EPOSTVARSLINGSTEKST : VIKTIG_ELLER_NULL_EPOSTVARSLINGSTEKST;
	}

}