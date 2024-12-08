package no.nav.foreldrepenger.tilganger.domene.ansatt;

import java.util.Set;

import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;

public record AnsattProfil(String brukernavn, String navn, String ansattVedEnhetId, Set<AnsattGruppe> ansattGrupper) {


    @Override
    public String toString() {
        return "BrukerProfil{" + "ansattGrupper=" + ansattGrupper + '}';
    }


}
