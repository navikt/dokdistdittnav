package no.nav.dokdistdittnav.qdist010.minsidevarsling;

import no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode;
import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.tms.varsel.action.EksternKanal.EPOST;
import static no.nav.tms.varsel.action.Sensitivitet.Substantial;
import static no.nav.tms.varsel.action.Varseltype.Oppgave;

public class OppgaveMapper extends AbstractVarselMapper {

	public static final String VEDTAK_VARSELTEKST = "Du har fått et vedtak som gjelder %s. Les vedtaket i dokumentarkivet. Hvis du ikke åpner og leser dokumentet, vil vi sende det til deg i posten.";
	public static final String VEDTAK_SMSVARSLINGSTEKST = "Hei! Du har fått et vedtak fra Nav. Logg inn på Nav for å lese det. Vennlig hilsen Nav";
	public static final String VEDTAK_EPOSTVARSLINGSTITTEL = "Vedtak fra Nav";
	public static final String VEDTAK_EPOSTVARSLINGSTEKST;

	public static final String VIKTIG_VARSELTEKST = "Du har fått et brev som du må lese: %s. Les brevet i dokumentarkivet. Hvis du ikke åpner og leser dokumentet, vil vi sende det til deg i posten.";
	public static final String VIKTIG_SMSVARSLINGSTEKST = "Hei! Du har fått et brev fra Nav som du må lese. Logg inn på Nav for å lese det. Vennlig hilsen Nav";
	public static final String VIKTIG_EPOSTVARSLINGSTITTEL = "Brev fra Nav";
	public static final String VIKTIG_EPOSTVARSLINGSTEKST;

	static {
		VEDTAK_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/vedtak_epostvarseltekst.html");
		VIKTIG_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/viktig_epostvarseltekst.html");
	}

	public static String opprettOppgave(HentForsendelseResponse forsendelse, String lenkeTilVarsel) {
		DistribusjonsTypeKode distribusjonstype = forsendelse.getDistribusjonstype();

		OpprettVarselBuilder opprettVarselBuilder = OpprettVarselBuilder.newInstance()
				.withType(Oppgave)
				.withVarselId(forsendelse.getBestillingsId())
				.withIdent(forsendelse.getMottaker().getMottakerId())
				.withLink(lenkeTilVarsel)
				.withSensitivitet(Substantial)
				.withAktivFremTil(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(SYNLIGEDAGER));

		if (distribusjonstype == null || distribusjonstype == VIKTIG) {
			return opprettVarselBuilder
					.withTekst(SPRAAKKODE_BOKMAAL, format(VIKTIG_VARSELTEKST, forsendelse.getForsendelseTittel()), true)
					.withEksternVarsling(OpprettVarselBuilder.eksternVarsling()
							.withPreferertKanal(EPOST)
							.withSmsVarslingstekst(VIKTIG_SMSVARSLINGSTEKST)
							.withEpostVarslingstittel(VIKTIG_EPOSTVARSLINGSTITTEL)
							.withEpostVarslingstekst(VIKTIG_EPOSTVARSLINGSTEKST)
							.withKanBatches(false)
							.withUtsettSendingTil(null)
					)
					.build();
		}

		if (distribusjonstype == VEDTAK) {
			return opprettVarselBuilder
					.withTekst(SPRAAKKODE_BOKMAAL, format(VEDTAK_VARSELTEKST, forsendelse.getForsendelseTittel()), true)
					.withEksternVarsling(OpprettVarselBuilder.eksternVarsling()
							.withPreferertKanal(EPOST)
							.withSmsVarslingstekst(VEDTAK_SMSVARSLINGSTEKST)
							.withEpostVarslingstittel(VEDTAK_EPOSTVARSLINGSTITTEL)
							.withEpostVarslingstekst(VEDTAK_EPOSTVARSLINGSTEKST)
							.withKanBatches(false)
							.withUtsettSendingTil(null))
					.build();
		}

		return "";
	}

}