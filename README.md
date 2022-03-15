# dokdistdittnav

[![team dokumentløsninger på Slack](https://img.shields.io/static/v1?label=slack&message=%23team_dokumentløsninger&color=blue)](https://nav-it.slack.com/archives/C6W9E5GPJ)



### dokdistdittnav

Denne applikasjonen fungerer som en proxy for NAV-applikasjoner som ønsker å kalle KRR for å hente digitale
kontaktopplysninger om Norges befolkning. Den kjører på GCP og støtter kun zero-trust sikkerhetsmekanismene som det
er lagt opp til via NAIS-plattformen, nemlig OAuth2.0 via Tokendings (TokenX) eller Azure AD.

I tillegg inkorporerer den støtte for de identknytningene som finnes i Persondataløsningen (PDL), slik at man kan spørre
på en hvilken som helst personident, enten det er DNR, FNR eller aktørId, og få svar på riktig person.