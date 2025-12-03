# Dokdistdittnav

Dokdistdittnav tilbyr tenester for distribusjon av sendingar til Min Side (nav.no) med ekstern varsling.
Dei tre tenestene er:

- kdist001 BehandleÅpnetDokument: Får beskjed når ein brukar har lese eit dokument. Inaktiverer varsel på Min Side, og oppdaterer interne statusar.
- kdist002 BehandleEksternNotifikasjonStatus: Fangar opp eksterne notifikasjonar som har feila, distribuerer dokumentet via sentral print, og inaktiverer varslinga for den feila Min Side-sendinga.
- qdist010 DistribuerForsendelseTilMinSide: Sending av varsel med ekstern notifikasjon til Min Side.

Dei tre tenestene les meldingar frå ulike stadar.

- kdist001 les hendingar frå Kafka-topic `teamdokumenthandtering.privat-dokdistdittnav-lestavmottaker`
- kdist002 les hendingar frå Kafka-topic `teamdokumenthandtering.aapen-dok-notifikasjon-status`
- qdist010 les hendingar frå MQ-kø `QA.P_DOKDISTDITTNAV.QDIST010_DIST_DITT_NAV`

Meir informasjon om korleis appen fungerer finn du
på [Confluence-sida for dokdistdittnav](https://confluence.adeo.no/display/BOA/dokdistdittnav).

### Førespurnadar

Spørsmål om koda eller prosjektet kan stillast
på [Slack-kanalen for \#Team Dokumentløysingar](https://nav-it.slack.com/archives/C6W9E5GPJ).