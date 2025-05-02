package no.nav.foreldrepenger.tilganger.tjenester.ansatt;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.tilganger.domene.ansatt.Ansatt;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattProfilTjeneste;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/ansatt")
public class AnsattInfoRestTjeneste {

    private AnsattProfilTjeneste ansattProfilTjeneste;
    private AnsattTjeneste ansattTjeneste;

    AnsattInfoRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattInfoRestTjeneste(AnsattProfilTjeneste tjeneste, AnsattTjeneste ansattTjeneste) {
        this.ansattProfilTjeneste = tjeneste;
        this.ansattTjeneste = ansattTjeneste;
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/ansatt-ident")
    public AnsattInfoResponseDto finnFraAnsattIdent(@NotNull @Valid AnsattIdentRequest request) {
        validerSystemKontekst();
        return ansattTjeneste.finnAnsatt(request.ansattIdent()).map(AnsattInfoRestTjeneste::mapTilProfilDto).orElse(null);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/ansatt-oid")
    public AnsattInfoResponseDto hentAnsattOid(@NotNull @Valid AnsattOidRequest request) {
        validerSystemKontekst();
        return ansattTjeneste.hentAnsatt(request.ansattOid).map(AnsattInfoRestTjeneste::mapTilProfilDto).orElse(null);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/grupper-medlem")
    public GrupperRespons grupperAlle(@NotNull @Valid GrupperMedlemRequest gruppeDto) {
        validerSystemKontekst();
        return new GrupperRespons(ansattProfilTjeneste.medlemAvGrupper(gruppeDto.ansattOid()));
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/grupper-filter")
    public GrupperRespons grupperMedlemskap(@NotNull @Valid GruppeFilterRequest gruppeDto) {
        validerSystemKontekst();
        return new GrupperRespons(ansattProfilTjeneste.medlemAvGrupper(gruppeDto.ansattOid, gruppeDto.grupper()));
    }

    public record GrupperRespons(@NotNull @Valid Set<AnsattGruppe> grupper) { }


    public record GrupperMedlemRequest(@NotNull UUID ansattOid) { }

    public record GruppeFilterRequest(@NotNull UUID ansattOid, @Valid @NotNull Set<AnsattGruppe> grupper) { }


    public record AnsattOidRequest(@NotNull UUID ansattOid) { }

    public record AnsattIdentRequest(@NotNull String ansattIdent) { }

    public record AnsattInfoResponseDto(@NotNull UUID ansattOid, @NotNull String ansattIdent, @NotNull String navn, String ansattVedEnhetId) {
    }

    private static AnsattInfoResponseDto mapTilProfilDto(Ansatt ansatt) {
        return new AnsattInfoResponseDto(ansatt.uid(), ansatt.ident(), ansatt.navn(), ansatt.ansattVedEnhetId());
    }


    private static void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger en gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }




}
