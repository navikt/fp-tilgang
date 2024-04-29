package no.nav.foreldrepenger.tilganger.domene;


import static no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph.NAVIDENT_PATTERN;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.GroupsResponse;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.User;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class BrukerInformasjonTjeneste {
    private static final Environment ENV = Environment.current();

    private AzureGraph azureGraph;

    private UUID oidSaksbehandler;
    private UUID oidVeileder;
    private UUID oidBeslutter;
    private UUID oidOverstyrer;
    private UUID oidOppgavestyrer;
    private UUID oidEgenAnsatt;
    private UUID oidKode6;
    private UUID oidKode7;
    private UUID oidDrifter;

    public BrukerInformasjonTjeneste() {
        // CDI
    }

    @Inject
    public BrukerInformasjonTjeneste(AzureGraph azureGraph,
                                     @KonfigVerdi(value = "gruppe.oid.saksbehandler") String saksbehandler,
                                     @KonfigVerdi(value = "gruppe.oid.veileder") String veileder,
                                     @KonfigVerdi(value = "gruppe.oid.beslutter") String beslutter,
                                     @KonfigVerdi(value = "gruppe.oid.overstyrer") String overstyrer,
                                     @KonfigVerdi(value = "gruppe.oid.oppgavestyrer") String oppgavestyrer,
                                     @KonfigVerdi(value = "gruppe.oid.egenansatt") String egenAnsatt,
                                     @KonfigVerdi(value = "gruppe.oid.kode6") String kode6,
                                     @KonfigVerdi(value = "gruppe.oid.kode7") String kode7,
                                     @KonfigVerdi(value = "gruppe.oid.drifter") String drifter
    ) {
        this.azureGraph = azureGraph;
        this.oidSaksbehandler = UUID.fromString(saksbehandler);
        this.oidVeileder = UUID.fromString(veileder);
        this.oidBeslutter = UUID.fromString(beslutter);
        this.oidOverstyrer = UUID.fromString(overstyrer);
        this.oidOppgavestyrer = UUID.fromString(oppgavestyrer);
        this.oidEgenAnsatt = UUID.fromString(egenAnsatt);
        this.oidKode6 = UUID.fromString(kode6);
        this.oidKode7 = UUID.fromString(kode7);
        this.oidDrifter = UUID.fromString(drifter);
    }

    /**
     * Henter informasjon for bruker logget inn i kontekst.
     */
    public BrukerInformasjon hentBrukerinformasjon() {
        var user = azureGraph.me();
        var grupper = azureGraph.memberOf();
        return mapBrukerInformasjon(user, grupper);
    }

    public BrukerInformasjon hentBrukerinformasjon(String ident) {
        if (ident == null || ident.isEmpty()) {
            throw new TekniskException("F-354885", "Kan ikke slå opp brukernavn uten å ha ident");
        }
        if (!ENV.isLocal() && !NAVIDENT_PATTERN.matcher(ident).matches()) {
            throw new TekniskException("F-281934", String.format("Mulig injection forsøk. Søkte med ugyldig ident '%s'", ident));
        }
        var user = azureGraph.user(ident);
        return getBrukerInformasjon(user);
    }

    public BrukerInformasjon hentBrukerinformasjon(UUID oid) {
        if (oid == null) {
            throw new TekniskException("F-364885", "Kan ikke slå opp brukernavn uten å ha oid");
        }
        var user = azureGraph.user(oid);
        return getBrukerInformasjon(user);
    }

    private BrukerInformasjon getBrukerInformasjon(Optional<User> user) {
        var grupper = user.map(u -> azureGraph.groups(u)).orElseThrow();
        return mapBrukerInformasjon(user.orElseThrow(), grupper);
    }

    private BrukerInformasjon mapBrukerInformasjon(User user, List<GroupsResponse.Group> grupper) {
        List<UUID> oidGrupper = grupper.stream().map(GroupsResponse.Group::id).toList();
        return new BrukerInformasjon.Builder(user.onPremisesSamAccountName(), user.displayName())
            .kanSaksbehandle(oidGrupper.contains(oidSaksbehandler))
            .kanVeilede(oidGrupper.contains(oidVeileder))
            .kanBeslutte(oidGrupper.contains(oidBeslutter))
            .kanOverstyre(oidGrupper.contains(oidOverstyrer))
            .kanOppgavestyre(oidGrupper.contains(oidOppgavestyrer))
            .kanBehandleKodeEgenAnsatt(oidGrupper.contains(oidEgenAnsatt))
            .kanBehandleKode6(oidGrupper.contains(oidKode6))
            .kanBehandleKode7(oidGrupper.contains(oidKode7))
            .kanDrifte(oidGrupper.contains(oidDrifter))
            .build();
    }
}
