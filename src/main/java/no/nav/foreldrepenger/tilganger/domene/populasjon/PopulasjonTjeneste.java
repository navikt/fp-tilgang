package no.nav.foreldrepenger.tilganger.domene.populasjon;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.felles.integrasjon.pdlpip.PersondataPipDto;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
public class PopulasjonTjeneste {


    private PopulasjonCache populasjonCache;

    PopulasjonTjeneste() {
        // CDI proxy
    }

    @Inject
    public PopulasjonTjeneste(PopulasjonCache populasjonCache) {
        this.populasjonCache = populasjonCache;
    }


    public TilgangVurdering vurderInternBruker(UUID ansattOID, Set<AnsattGruppe> kreverGrupper,
                                               Set<String> personIdenter, Set<String> aktørIdenter) {
        var sjekkGrupper = new HashSet<>(kreverGrupper);

        // Finn behov for ekstra AD-grupper for tilfelle av adressebeskyttelse eller skjerming
        var allePersonPips = populasjonCache.finnPdlPipFor(personIdenter, aktørIdenter);
        if (allePersonPips.stream().anyMatch(PersondataPipDto::harStrengAdresseBeskyttelse)) {
            sjekkGrupper.add(AnsattGruppe.STRENGTFORTROLIG);
        } else if (allePersonPips.stream().anyMatch(PersondataPipDto::harAdresseBeskyttelse)) {
            sjekkGrupper.add(AnsattGruppe.FORTROLIG);
        }
        if (populasjonCache.finnSkjermingFor(allePersonPips)) {
           sjekkGrupper.add(AnsattGruppe.SKJERMET);
        }

        // Tidlig godkjenning dersom det ikke skal sjekkes om ansatt er med i AD-grupper
        if (sjekkGrupper.isEmpty()) {
            return TilgangVurdering.godkjenn();
        }

        var harAnsattGrupper = populasjonCache.finnAnsattGrupperFor(ansattOID);
        if (sjekkGrupper.contains(AnsattGruppe.STRENGTFORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.STRENGTFORTROLIG)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_KODE_6);
        } else if (sjekkGrupper.contains(AnsattGruppe.FORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.FORTROLIG)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_KODE_7);
        } else if (sjekkGrupper.contains(AnsattGruppe.SKJERMET) && !harAnsattGrupper.contains(AnsattGruppe.SKJERMET)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_EGEN_ANSATT);
        } else {
            var manglende = sjekkGrupper.stream().filter(g -> !harAnsattGrupper.contains(g)).toList();
            return manglende.isEmpty() ? TilgangVurdering.godkjenn() : TilgangVurdering.avslåGenerell("Ikke med i grupper " + manglende);
        }
    }

    public TilgangVurdering vurderEksternBruker(String subjectPersonIdent, Set<String> personIdenter, Set<String> aktørIdenter) {
        var subjectPip = populasjonCache.finnPdlPipFor(subjectPersonIdent);
        if (subjectPip.erIkkeMyndig()) {
            return TilgangVurdering.avslåGenerell("Ikke gammel nok");
        }
        var subjectAktørId = subjectPip.aktørId();
        if (personIdenter.size() == 1 && personIdenter.stream().anyMatch(pi -> Objects.equals(pi, subjectPersonIdent))) {
            return TilgangVurdering.godkjenn();
        }
        if (aktørIdenter.size() == 1 && aktørIdenter.stream().anyMatch(ai -> Objects.equals(ai, subjectAktørId))) {
            return TilgangVurdering.godkjenn();
        }
        return TilgangVurdering.avslåGenerell("Har bare tilgang til seg selv");
    }
}
