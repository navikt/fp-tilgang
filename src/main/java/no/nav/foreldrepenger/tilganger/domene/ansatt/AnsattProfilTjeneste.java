package no.nav.foreldrepenger.tilganger.domene.ansatt;


import static no.nav.foreldrepenger.tilganger.utils.RegexUtils.NAVIDENT_PATTERN;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;

@Dependent
public class AnsattProfilTjeneste {
    private static final Environment ENV = Environment.current();

    private AnsattTjeneste ansattTjeneste;

    public AnsattProfilTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattProfilTjeneste(AnsattTjeneste ansattTjeneste) {
        this.ansattTjeneste = ansattTjeneste;
    }

    /**
     * Henter informasjon for bruker logget inn i kontekst.
     * Trenger en gyldig OBO azure token.
     */
    public AnsattProfil hentProfil() {
        var ansatt = ansattTjeneste.hentAnsattFraKontekst();
        var grupper = ansattTjeneste.hentGrupperFraKontekst(Gruppe.getAlleGrupper());
        return mapAnsattProfil(ansatt.orElseThrow(), grupper);
    }

    public AnsattProfil hentProfil(String ident) {
        if (ident == null || ident.isEmpty()) {
            throw new TekniskException("F-354885", "Kan ikke slå opp brukernavn uten å ha ident");
        }
        if (!ENV.isLocal() && !NAVIDENT_PATTERN.matcher(ident).matches()) {
            throw new TekniskException("F-281934", String.format("Mulig injection forsøk. Søkte med ugyldig ident '%s'", ident));
        }
        var ansatt = ansattTjeneste.hentAnsatt(ident);
        return getAnsattProfil(ansatt);
    }

    public AnsattProfil hentProfil(UUID oid) {
        if (oid == null) {
            throw new TekniskException("F-364885", "Kan ikke slå opp brukernavn uten å ha uid");
        }
        var ansatt = ansattTjeneste.hentAnsatt(oid);
        return getAnsattProfil(ansatt);
    }

    private AnsattProfil getAnsattProfil(Optional<Ansatt> ansatt) {
        var grupper = ansatt.map(u -> ansattTjeneste.hentGrupper(u, Gruppe.getAlleGrupper())).orElseThrow(() -> new IllegalStateException("Fant ikke bruker"));
        return mapAnsattProfil(ansatt.orElseThrow(), grupper);
    }

    private AnsattProfil mapAnsattProfil(Ansatt ansatt, List<UUID> grupper) {
        return new AnsattProfil.Builder(ansatt.ident(), ansatt.navn(), ansatt.epost())
            .kanSaksbehandle(grupper.contains(Gruppe.SAKSBEHNADLER.getId()))
            .kanVeilede(grupper.contains(Gruppe.VEILEDER.getId()))
            .kanBeslutte(grupper.contains(Gruppe.BESLUTTER.getId()))
            .kanOverstyre(grupper.contains(Gruppe.OVERSTYRER.getId()))
            .kanOppgavestyre(grupper.contains(Gruppe.OPPGAVESTYRER.getId()))
            .kanBehandleKodeEgenAnsatt(grupper.contains(Gruppe.EGENANSATT.getId()))
            .kanBehandleKode6(grupper.contains(Gruppe.KODE6.getId()))
            .kanBehandleKode7(grupper.contains(Gruppe.KODE7.getId()))
            .kanDrifte(grupper.contains(Gruppe.DRIFTER.getId()))
            .build();
    }
}
