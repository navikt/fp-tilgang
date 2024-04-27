package no.nav.foreldrepenger.tilganger.domene;


import static no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph.NAVIDENT_PATTERN;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.AzureGraph;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.GroupsResponse;
import no.nav.foreldrepenger.tilganger.integrasjoner.azure.User;
import no.nav.vedtak.exception.TekniskException;

@Dependent
public class BrukerInformasjonTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BrukerInformasjonTjeneste.class);
    private static final Environment ENV = Environment.current();

    private AzureGraph azureGraph;

    private UUID gruppenavnSaksbehandler;
    private UUID gruppenavnVeileder;
    private UUID gruppenavnBeslutter;
    private UUID gruppenavnOverstyrer;
    private UUID gruppenavnOppgavestyrer;
    private UUID gruppenavnEgenAnsatt;
    private UUID gruppenavnKode6;
    private UUID gruppenavnKode7;
    private UUID gruppenavnDrifter;

    public BrukerInformasjonTjeneste() {
        // CDI
    }

    @Inject
    public BrukerInformasjonTjeneste(AzureGraph azureGraph,
                                     @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler") String gruppenavnSaksbehandler,
                                     @KonfigVerdi(value = "bruker.gruppenavn.veileder") String gruppenavnVeileder,
                                     @KonfigVerdi(value = "bruker.gruppenavn.beslutter") String gruppenavnBeslutter,
                                     @KonfigVerdi(value = "bruker.gruppenavn.overstyrer") String gruppenavnOverstyrer,
                                     @KonfigVerdi(value = "bruker.gruppenavn.oppgavestyrer") String gruppenavnOppgavestyrer,
                                     @KonfigVerdi(value = "bruker.gruppenavn.egenansatt") String gruppenavnEgenAnsatt,
                                     @KonfigVerdi(value = "bruker.gruppenavn.kode6") String gruppenavnKode6,
                                     @KonfigVerdi(value = "bruker.gruppenavn.kode7") String gruppenavnKode7,
                                     @KonfigVerdi(value = "bruker.gruppenavn.drifter") String gruppenavnDrifter
    ) {
        this.azureGraph = azureGraph;
        this.gruppenavnSaksbehandler = UUID.fromString(gruppenavnSaksbehandler);
        this.gruppenavnVeileder = UUID.fromString(gruppenavnVeileder);
        this.gruppenavnBeslutter = UUID.fromString(gruppenavnBeslutter);
        this.gruppenavnOverstyrer = UUID.fromString(gruppenavnOverstyrer);
        this.gruppenavnOppgavestyrer = UUID.fromString(gruppenavnOppgavestyrer);
        this.gruppenavnEgenAnsatt = UUID.fromString(gruppenavnEgenAnsatt);
        this.gruppenavnKode6 = UUID.fromString(gruppenavnKode6);
        this.gruppenavnKode7 = UUID.fromString(gruppenavnKode7);
        this.gruppenavnDrifter = UUID.fromString(gruppenavnDrifter);
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
        return mapBrukerInformasjon(user.get(), grupper);
    }

    private BrukerInformasjon mapBrukerInformasjon(User user, List<GroupsResponse.Group> grupper) {
        List<UUID> filtrerGrupper = grupper.stream().map(GroupsResponse.Group::id).toList();
        return new BrukerInformasjon.Builder(user.onPremisesSamAccountName(), user.displayName())
            .kanSaksbehandle(filtrerGrupper.contains(gruppenavnSaksbehandler))
            .kanVeilede(filtrerGrupper.contains(gruppenavnVeileder))
            .kanBeslutte(filtrerGrupper.contains(gruppenavnBeslutter))
            .kanOverstyre(filtrerGrupper.contains(gruppenavnOverstyrer))
            .kanOppgavestyre(filtrerGrupper.contains(gruppenavnOppgavestyrer))
            .kanBehandleKodeEgenAnsatt(filtrerGrupper.contains(gruppenavnEgenAnsatt))
            .kanBehandleKode6(filtrerGrupper.contains(gruppenavnKode6))
            .kanBehandleKode7(filtrerGrupper.contains(gruppenavnKode7))
            .kanDrifte(filtrerGrupper.contains(gruppenavnDrifter))
            .build();
    }
}
