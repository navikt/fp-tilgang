package no.nav.foreldrepenger.tilganger.domene;


import static no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph.NAVIDENT_PATTERN;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.TekniskException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class AnsattProfilTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AnsattProfilTjeneste.class);
    private static final Environment ENV = Environment.current();

    private UUID oidSaksbehandler;
    private UUID oidVeileder;
    private UUID oidBeslutter;
    private UUID oidOverstyrer;
    private UUID oidOppgavestyrer;
    private UUID oidEgenAnsatt;
    private UUID oidKode6;
    private UUID oidKode7;
    private UUID oidDrifter;

    private AnsattTjeneste ansattTjeneste;

    public AnsattProfilTjeneste() {
        // CDI
    }

    @Inject
    public AnsattProfilTjeneste(AnsattTjeneste ansattTjeneste,
                                @KonfigVerdi(value = "gruppe.oid.saksbehandler") String saksbehandler,
                                @KonfigVerdi(value = "gruppe.oid.veileder") String veileder,
                                @KonfigVerdi(value = "gruppe.oid.beslutter") String beslutter,
                                @KonfigVerdi(value = "gruppe.oid.overstyrer") String overstyrer,
                                @KonfigVerdi(value = "gruppe.oid.oppgavestyrer") String oppgavestyrer,
                                @KonfigVerdi(value = "gruppe.oid.egenansatt") String egenAnsatt,
                                @KonfigVerdi(value = "gruppe.oid.kode6") String kode6,
                                @KonfigVerdi(value = "gruppe.oid.kode7") String kode7,
                                @KonfigVerdi(value = "gruppe.oid.drifter") String drifter) {
        this.oidSaksbehandler = UUID.fromString(saksbehandler);
        this.oidVeileder = UUID.fromString(veileder);
        this.oidBeslutter = UUID.fromString(beslutter);
        this.oidOverstyrer = UUID.fromString(overstyrer);
        this.oidOppgavestyrer = UUID.fromString(oppgavestyrer);
        this.oidEgenAnsatt = UUID.fromString(egenAnsatt);
        this.oidKode6 = UUID.fromString(kode6);
        this.oidKode7 = UUID.fromString(kode7);
        this.oidDrifter = UUID.fromString(drifter);
        this.ansattTjeneste = ansattTjeneste;
    }

    /**
     * Henter informasjon for bruker logget inn i kontekst.
     * Trenger en gyldig OBO azure token.
     */
    public AnsattProfil hentProfil() {
        var ansatt = ansattTjeneste.hentAnsattFraKontekst();
        var grupper = ansattTjeneste.hentGrupperFraKontekst();
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
        var grupper = ansatt.map(u -> ansattTjeneste.hentGrupper(u)).orElseThrow(() -> new IllegalStateException("Fant ikke bruker"));
        return mapAnsattProfil(ansatt.orElseThrow(), grupper);
    }

    private AnsattProfil mapAnsattProfil(Ansatt ansatt, List<UUID> grupper) {
        return new AnsattProfil.Builder(ansatt.ident(), ansatt.navn(), ansatt.epost())
            .kanSaksbehandle(grupper.contains(oidSaksbehandler))
            .kanVeilede(grupper.contains(oidVeileder))
            .kanBeslutte(grupper.contains(oidBeslutter))
            .kanOverstyre(grupper.contains(oidOverstyrer))
            .kanOppgavestyre(grupper.contains(oidOppgavestyrer))
            .kanBehandleKodeEgenAnsatt(grupper.contains(oidEgenAnsatt))
            .kanBehandleKode6(grupper.contains(oidKode6))
            .kanBehandleKode7(grupper.contains(oidKode7))
            .kanDrifte(grupper.contains(oidDrifter))
            .build();
    }
}
