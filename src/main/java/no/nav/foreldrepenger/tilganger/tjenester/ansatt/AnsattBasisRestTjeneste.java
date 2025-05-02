package no.nav.foreldrepenger.tilganger.tjenester.ansatt;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

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
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Brukes av applikasjoner som kun trenger basisinformasjon (navn, enhet) om ansatte
 */
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/ansatt/basis")
public class AnsattBasisRestTjeneste {

    private AnsattTjeneste ansattTjeneste;

    AnsattBasisRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattBasisRestTjeneste(AnsattTjeneste ansattTjeneste) {
        this.ansattTjeneste = ansattTjeneste;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/kontekst")
    public BrukerProfilResponseDto brukerProfilUtvidet() {
        validerBrukerKontekst();
        return mapTilProfilDto(ansattTjeneste.hentAnsattFraKontekst());
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/navident")
    public BrukerProfilResponseDto finnUserFraNavIdent(@NotNull @Valid AnsattBasisRestTjeneste.ProfilIdentRequest request) {
        validerSystemKontekst();
        return mapTilProfilDto(ansattTjeneste.finnAnsatt(request.ident()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/uid")
    public BrukerProfilResponseDto hentUserUid(@NotNull @Valid AnsattBasisRestTjeneste.ProfilUidRequest request) {
        validerSystemKontekst();
        return mapTilProfilDto(ansattTjeneste.hentAnsatt(request.uid()));
    }

    public record ProfilUidRequest(@NotNull UUID uid) { }

    public record ProfilIdentRequest(@NotNull String ident) { }

    public record BrukerProfilResponseDto(@NotNull UUID uid, @NotNull String ident, @NotNull String navn, String ansattVedEnhetId) {
    }

    private BrukerProfilResponseDto mapTilProfilDto(Optional<Ansatt> ansatt) {
        return ansatt.map(a -> new BrukerProfilResponseDto(a.uid(), a.ident(), a.navn(), a.ansattVedEnhetId()))
            .orElseThrow(() -> new IllegalStateException("Bruker finnes ikke."));
    }

    private static void validerBrukerKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!IdentType.InternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new WebApplicationException("Trenger en gyldig OBO token.", Response.Status.FORBIDDEN);
        }
    }

    private static void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger en gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }

}
