package no.nav.foreldrepenger.tilganger.tjenester.ansatt;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
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

import no.nav.foreldrepenger.tilganger.domene.ansatt.Ansatt;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattProfil;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattProfilTjeneste;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/bruker/profil")
public class AnsattProfilRestTjeneste {

    private AnsattProfilTjeneste ansattProfilTjeneste;
    private AnsattTjeneste ansattTjeneste;

    AnsattProfilRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattProfilRestTjeneste(AnsattProfilTjeneste tjeneste,
                                    AnsattTjeneste ansattTjeneste) {
        this.ansattProfilTjeneste = tjeneste;
        this.ansattTjeneste = ansattTjeneste;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/utvidet")
    public BrukerProfilUtvidetResponseDto brukerProfilUtvidet() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!IdentType.InternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new WebApplicationException("Trenger en gyldig OBO token.", Response.Status.FORBIDDEN);
        }
        return mapTilUtvidetProfilDto(ansattProfilTjeneste.hentProfil());
    }

    @POST
    @Produces(APPLICATION_JSON)
    public BrukerProfilResponseDto finnUser(@NotNull @Valid AnsattProfilRestTjeneste.ProfilIdentRequest request) {
        return mapTilProfilDto(ansattTjeneste.hentAnsatt(request.ident()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/navident")
    public BrukerProfilResponseDto finnUserFraNavIdent(@NotNull @Valid AnsattProfilRestTjeneste.ProfilIdentRequest request) {
        return mapTilProfilDto(ansattTjeneste.hentAnsatt(request.ident()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/uid")
    public BrukerProfilResponseDto hentUserUid(@NotNull @Valid AnsattProfilRestTjeneste.ProfilUidRequest request) {
        return mapTilProfilDto(ansattTjeneste.hentAnsatt(request.uid()));
    }

    public record ProfilUidRequest(@NotNull UUID uid) {
    }

    public record ProfilIdentRequest(@NotNull String ident) {
    }

    public record BrukerProfilResponseDto(@NotNull UUID uid, @NotNull String ident, @NotNull String navn, String fornavnEtternavn, String ansattVedEnhetId) {
    }

    public record BrukerProfilUtvidetResponseDto(@NotNull String brukernavn,
                                                 @NotNull String navn,
                                                 String fornavnEtternavn,
                                                 String ansattVedEnhetId,
                                                 boolean kanSaksbehandle,
                                                 boolean kanVeilede,
                                                 boolean kanBeslutte,
                                                 boolean kanOverstyre,
                                                 boolean kanOppgavestyre,
                                                 boolean kanBehandleKode6,
                                                 LocalDateTime funksjonellTid) {
    }

    private BrukerProfilResponseDto mapTilProfilDto(Optional<Ansatt> ansatt) {
        return ansatt.map(a -> new BrukerProfilResponseDto(a.uid(), a.ident(), a.navn(), a.fornavnEtternavn(), a.ansattVedEnhetId()))
            .orElseThrow(() -> new IllegalStateException("Bruker finnes ikke."));
    }

    private BrukerProfilUtvidetResponseDto mapTilUtvidetProfilDto(AnsattProfil brukerProfil) {
        return new BrukerProfilUtvidetResponseDto(brukerProfil.brukernavn(),
            brukerProfil.navn(),
            brukerProfil.fornavnEtternavn(),
            brukerProfil.ansattVedEnhetId(),
            brukerProfil.kanSaksbehandle(),
            brukerProfil.kanVeilede(),
            brukerProfil.kanBeslutte(),
            brukerProfil.kanOverstyre(),
            brukerProfil.kanOppgavestyre(),
            brukerProfil.kanBehandleKode6(),
            brukerProfil.funksjonellTid());
    }
}
