package no.nav.dokdistdittnav.qdist010;

import no.nav.dokdistdittnav.consumer.rdist001.to.HentForsendelseResponse;
import no.nav.dokdistdittnav.exception.functional.VarseltekstfilNotFoundException;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.String.format;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdittnav.consumer.rdist001.kodeverk.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdittnav.utils.DokdistUtils.classpathToString;
import static no.nav.tms.varsel.action.EksternKanal.EPOST;
import static no.nav.tms.varsel.action.Sensitivitet.Substantial;
import static no.nav.tms.varsel.action.Varseltype.Beskjed;
import static no.nav.tms.varsel.action.Varseltype.Oppgave;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

public class ForsendelseMapper {

	public static final String AARSOPPGAVE_DOKUMENTTYPEID = "000053";
	public static final Integer SYNLIGEDAGER = 10;
	public static final String SPRAAKKODE_BOKMAAL = "nb";

	public static final String VEDTAK_VARSELTEKST = "Du har fått et vedtak som gjelder %s. Les vedtaket i dokumentarkivet. Hvis du ikke åpner og leser dokumentet, vil vi sende det til deg i posten.";
	public static final String VIKTIG_VARSELTEKST = "Du har fått et brev som du må lese: %s. Les brevet i dokumentarkivet. Hvis du ikke åpner og leser dokumentet, vil vi sende det til deg i posten.";
	public static final String ANNET_VARSELTEKST = "Du har fått en melding som gjelder %s. Les meldingen i dokumentarkivet.";

	public static final String VEDTAK_SMSVARSLINGSTEKST = "Hei! Du har fått et vedtak fra Nav. Logg inn på Nav for å lese det. Vennlig hilsen Nav";
	public static final String VIKTIG_SMSVARSLINGSTEKST = "Hei! Du har fått et brev fra Nav som du må lese. Logg inn på Nav for å lese det. Vennlig hilsen Nav";
	public static final String ANNET_SMSVARSLINGSTEKST = "Hei! Du har fått en melding fra Nav. Logg inn på Nav for å lese den. Vennlig hilsen Nav";
	public static final String AARSOPPGAVE_SMSVARSLINGSTEKST = "Hei! Du har fått en årsoppgave fra Nav. Logg inn på Nav for å lese den. Vennlig hilsen Nav";

	public static final String VEDTAK_EPOSTVARSLINGSTITTEL = "Vedtak fra Nav";
	public static final String VIKTIG_EPOSTVARSLINGSTITTEL = "Brev fra Nav";
	public static final String ANNET_EPOSTVARSLINGSTITTEL = "Melding fra Nav";
	public static final String AARSOPPGAVE_EPOSTVARSLINGSTITTEL = "Årsoppgave fra Nav";

	public static final String VEDTAK_EPOSTVARSLINGSTEKST;
	public static final String VIKTIG_EPOSTVARSLINGSTEKST;
	public static final String ANNET_EPOSTVARSLINGSTEKST;
	public static final String AARSOPPGAVE_EPOSTVARSLINGSTEKST;


	static {
		VEDTAK_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/vedtak_epostvarseltekst.html");
		VIKTIG_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/viktig_epostvarseltekst.html");
		ANNET_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/melding_epostvarseltekst.html");
		AARSOPPGAVE_EPOSTVARSLINGSTEKST = getFileAndAssertNotNullOrEmpty("varseltekster/aarsoppgave_epostvarseltekst.html");
	}

	public static String opprettBeskjed(String url, HentForsendelseResponse forsendelse) {
		String dokumenttypeId = forsendelse.getDokumenter().getFirst().getDokumenttypeId();

		return OpprettVarselBuilder.newInstance()
				.withType(Beskjed)
				.withVarselId(forsendelse.getBestillingsId())
				.withIdent(forsendelse.getMottaker().getMottakerId())
				.withTekst(SPRAAKKODE_BOKMAAL, format(ANNET_VARSELTEKST, forsendelse.getForsendelseTittel()), true)
				.withLink(lagLenkeMedTemaOgArkivId(url, forsendelse))
				.withSensitivitet(Substantial)
				.withAktivFremTil(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(SYNLIGEDAGER)) // UTC i staden for lokaltid - tilrådd av doken
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

	public static String opprettOppgave(String url, HentForsendelseResponse forsendelse) {
		var distribusjonstype = forsendelse.getDistribusjonstype();

		OpprettVarselBuilder opprettVarselBuilder = OpprettVarselBuilder.newInstance()
				.withType(Oppgave)
				.withVarselId(forsendelse.getBestillingsId())
				.withIdent(forsendelse.getMottaker().getMottakerId())
				.withLink(lagLenkeMedTemaOgArkivId(url, forsendelse))
				.withSensitivitet(Substantial)
				.withAktivFremTil(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(SYNLIGEDAGER)); // UTC i staden for lokaltid - tilrådd av doken

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

	private static String lagLenkeMedTemaOgArkivId(String url, HentForsendelseResponse hentForsendelseResponse) {
		URI uri = UriComponentsBuilder
				.fromUriString(url)
				.path(hentForsendelseResponse.getTema() + "/" + hentForsendelseResponse.getArkivInformasjon().getArkivId())
				.build().toUri();

		return uri.toString();
	}

	private static String getFileAndAssertNotNullOrEmpty(String path) {
		String result = classpathToString(path);
		if (isEmpty(result)) {
			throw new VarseltekstfilNotFoundException("Fant ikke filen på path: " + path);
		}
		return result;
	}
}