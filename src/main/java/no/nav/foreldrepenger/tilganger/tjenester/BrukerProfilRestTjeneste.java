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

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/informasjon")
    public BrukerProfilUtvidetResponseDto brukerProfilUtvidet() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!IdentType.InternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new WebApplicationException("Trenger en gyldig OBO token.", Response.Status.FORBIDDEN);
        }
        return mapTilUtvidetProfilDto(brukerProfilTjeneste.hentProfil());
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/profil")
    public BrukerProfilResponseDto finnUser(@NotNull @Valid BrukerProfilRestTjeneste.ProfilIdentRequest request) {
        return mapTilProfilDto(brukerProfilTjeneste.hentProfil(request.ident()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/profil/uid")
    public BrukerProfilResponseDto finnUserV2(@NotNull @Valid BrukerProfilRestTjeneste.ProfilUidRequest request) {
        return mapTilProfilDto(brukerProfilTjeneste.hentProfil(request.uid()));
    }

    public record ProfilUidRequest(@NotNull UUID uid) {}

    public record ProfilIdentRequest(@NotNull String ident) {}

    public record BrukerProfilResponseDto(@NotNull String ident, @NotNull String navn, String epost) {}

    public record BrukerProfilUtvidetResponseDto(@NotNull String brukernavn,
                                                 @NotNull String navn,
                                                 boolean kanSaksbehandle,
                                                 boolean kanVeilede,
                                                 boolean kanBeslutte,
                                                 boolean kanOverstyre,
                                                 boolean kanOppgavestyre,
                                                 boolean kanBehandleKode6,
                                                 LocalDateTime funksjonellTid) {}

    private BrukerProfilResponseDto mapTilProfilDto(BrukerProfil brukerProfil) {
        return new BrukerProfilResponseDto(brukerProfil.brukernavn(), brukerProfil.navn(), brukerProfil.epost());
    }

    private BrukerProfilUtvidetResponseDto mapTilUtvidetProfilDto(BrukerProfil brukerProfil) {
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
