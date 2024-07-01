package no.nav.foreldrepenger.tilganger.tjenester;

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
import no.nav.foreldrepenger.tilganger.domene.BrukerProfil;
import no.nav.foreldrepenger.tilganger.domene.BrukerProfilTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/bruker")
public class BrukerProfilRestTjeneste {

    private BrukerProfilTjeneste brukerProfilTjeneste;

    BrukerProfilRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public BrukerProfilRestTjeneste(BrukerProfilTjeneste tjeneste) {
        this.brukerProfilTjeneste = tjeneste;
    }

    /**
     * @deprecated bruk meUserV2
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("/fraKontekst")
    @Deprecated(forRemoval = true)
    public Response meUser() {
        validerTilgang();
        return Response.ok(mapTilUtvidetDto(brukerProfilTjeneste.hentBrukerProfil())).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/informasjon")
    public Response meUserV2() {
        validerTilgang();
        return Response.ok(mapTilUtvidetDto(brukerProfilTjeneste.hentBrukerProfil())).build();
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/profil")
    public Response finnUser(@NotNull @Valid ProfilIdentRequest request) {
        return Response.ok(mapTilDto(brukerProfilTjeneste.hentBrukerProfil(request.ident()))).build();
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/profil/v2")
    public Response finnUserV2(@NotNull @Valid ProfilUuidRequest request) {
        return Response.ok(mapTilDto(brukerProfilTjeneste.hentBrukerProfil(request.oid()))).build();
    }

    private static void validerTilgang() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!IdentType.InternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new WebApplicationException("Trenger en gyldig OBO token.", Response.Status.FORBIDDEN);
        }
    }

    public record ProfilUuidRequest(@NotNull UUID oid) {}

    public record ProfilIdentRequest(@NotNull String ident) {}

    public record BrukerProfilResponseDto(@NotNull String ident, @NotNull String navn, String epostAdresse) {}

    public record BrukerProfilUtvidetResponseDto(@NotNull String brukernavn,
                                                 @NotNull String navn,
                                                 boolean kanSaksbehandle,
                                                 boolean kanVeilede,
                                                 boolean kanBeslutte,
                                                 boolean kanOverstyre,
                                                 boolean kanOppgavestyre,
                                                 boolean kanBehandleKode6,
                                                 LocalDateTime funksjonellTid) {}

    private BrukerProfilResponseDto mapTilDto(BrukerProfil brukerProfil) {
        return new BrukerProfilResponseDto(brukerProfil.brukernavn(), brukerProfil.navn(), brukerProfil.epost());
    }

    private BrukerProfilUtvidetResponseDto mapTilUtvidetDto(BrukerProfil brukerProfil) {
        return new BrukerProfilUtvidetResponseDto(
            brukerProfil.brukernavn(),
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
