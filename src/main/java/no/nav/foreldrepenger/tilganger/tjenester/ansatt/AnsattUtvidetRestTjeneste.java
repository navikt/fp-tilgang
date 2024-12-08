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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattProfil;
import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattProfilTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/ansatt/utvidet")
public class AnsattUtvidetRestTjeneste {

    private AnsattProfilTjeneste ansattProfilTjeneste;

    AnsattUtvidetRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public AnsattUtvidetRestTjeneste(AnsattProfilTjeneste tjeneste) {
        this.ansattProfilTjeneste = tjeneste;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/kontekst")
    public AnsattProfilUtvidetDto brukerProfilUtvidet() {
        validerBrukerKontekst();
        return mapTilUtvidetProfilDto(ansattProfilTjeneste.hentProfil());
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/gruppemedlemskap-kontekst")
    public GruppeDto erMedlemAvGrupper(@NotNull @Valid GruppeDto gruppeDto) {
        validerBrukerKontekst();
        return new GruppeDto(ansattProfilTjeneste.medlemAvGrupper(gruppeDto.grupper()));
    }

    public record GruppeDto(@NotNull @Valid Set<AnsattGruppe> grupper) { }


    public record UidGruppeDto(@NotNull UUID uid, @Valid @NotNull Set<AnsattGruppe> grupper) { }


    public record AnsattProfilUtvidetDto(@NotNull String brukernavn, @NotNull String navn, String ansattVedEnhetId, Set<AnsattGruppe> ansattGrupper) {
    }

    private AnsattProfilUtvidetDto mapTilUtvidetProfilDto(AnsattProfil brukerProfil) {
        return new AnsattProfilUtvidetDto(brukerProfil.brukernavn(), brukerProfil.navn(), brukerProfil.ansattVedEnhetId(), brukerProfil.ansattGrupper());
    }

    private static void validerBrukerKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!IdentType.InternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new WebApplicationException("Trenger en gyldig OBO token.", Response.Status.FORBIDDEN);
        }
    }
}
