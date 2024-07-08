package no.nav.foreldrepenger.tilganger.tjenester.ansatt;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattProfil;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattProfilTjeneste;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/bruker")
public class AnsattProfilRestTjeneste {

    private AnsattProfilTjeneste ansattProfilTjeneste;

    AnsattProfilRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattProfilRestTjeneste(AnsattProfilTjeneste tjeneste) {
        this.ansattProfilTjeneste = tjeneste;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/informasjon")
    public BrukerProfilUtvidetResponseDto brukerProfilUtvidet() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!IdentType.InternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new WebApplicationException("Trenger en gyldig OBO token.", Response.Status.FORBIDDEN);
        }
        return mapTilUtvidetProfilDto(ansattProfilTjeneste.hentProfil());
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/profil")
    public BrukerProfilResponseDto finnUser(@NotNull @Valid AnsattProfilRestTjeneste.ProfilIdentRequest request) {
        return mapTilProfilDto(ansattProfilTjeneste.hentProfil(request.ident()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/profil/uid")
    @UtenAutentisering
    public BrukerProfilResponseDto finnUserV2(@NotNull @Valid AnsattProfilRestTjeneste.ProfilUidRequest request) {
        return mapTilProfilDto(ansattProfilTjeneste.hentProfil(request.uid()));
    }

    public record ProfilUidRequest(@NotNull UUID uid) {
    }

    public record ProfilIdentRequest(@NotNull String ident) {
    }

    public record BrukerProfilResponseDto(@NotNull String ident, @NotNull String navn, String epost) {
    }

    public record BrukerProfilUtvidetResponseDto(@NotNull String brukernavn,
                                                 @NotNull String navn,
                                                 boolean kanSaksbehandle,
                                                 boolean kanVeilede,
                                                 boolean kanBeslutte,
                                                 boolean kanOverstyre,
                                                 boolean kanOppgavestyre,
                                                 boolean kanBehandleKode6,
                                                 LocalDateTime funksjonellTid) {
    }

    private BrukerProfilResponseDto mapTilProfilDto(AnsattProfil brukerProfil) {
        return new BrukerProfilResponseDto(brukerProfil.brukernavn(), brukerProfil.navn(), brukerProfil.epost());
    }

    private BrukerProfilUtvidetResponseDto mapTilUtvidetProfilDto(AnsattProfil brukerProfil) {
        return new BrukerProfilUtvidetResponseDto(brukerProfil.brukernavn(),
            brukerProfil.navn(),
            brukerProfil.kanSaksbehandle(),
            brukerProfil.kanVeilede(),
            brukerProfil.kanBeslutte(),
            brukerProfil.kanOverstyre(),
            brukerProfil.kanOppgavestyre(),
            brukerProfil.kanBehandleKode6(),
            brukerProfil.funksjonellTid());
    }
}
