package no.nav.foreldrepenger.tilganger.tjenester.populasjon;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Objects;
import java.util.Optional;
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

import no.nav.foreldrepenger.tilganger.domene.populasjon.PopulasjonTjeneste;
import no.nav.foreldrepenger.tilganger.domene.populasjon.TilgangResultat;
import no.nav.foreldrepenger.tilganger.domene.populasjon.TilgangVurdering;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/populasjon")
public class PopulasjonRestTjeneste {

    private PopulasjonTjeneste populasjonTjeneste;

    PopulasjonRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public PopulasjonRestTjeneste(PopulasjonTjeneste populasjonTjeneste) {
        this.populasjonTjeneste = populasjonTjeneste;
    }


    @POST
    @Produces(APPLICATION_JSON)
    @Path("/internbruker")
    public TilgangsvurderingDto sjekkInternBruker(@NotNull @Valid PopulasjonRestTjeneste.PopulasjonInternRequest request) {
        validerSystemKontekst();
        var vurdering = populasjonTjeneste.vurderInternBruker(request.ansattOid(),
            Optional.ofNullable(request.personIdenter()).orElseGet(Set::of), Optional.ofNullable(request.aktørIdenter()).orElseGet(Set::of));
        return mapTilVurderingDto(vurdering);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/eksternbruker")
    public TilgangsvurderingDto sjekkEksternBruker(@NotNull @Valid PopulasjonRestTjeneste.PopulasjonEksternRequest request) {
        validerSystemKontekst();
        var vurdering = populasjonTjeneste.vurderEksternBruker(request.subjectPersonIdent(),
            Optional.ofNullable(request.personIdenter()).orElseGet(Set::of), Optional.ofNullable(request.aktørIdenter()).orElseGet(Set::of));
        return mapTilVurderingDto(vurdering);
    }

    public record PopulasjonEksternRequest(@NotNull String subjectPersonIdent,
                                           @Valid Set<String> personIdenter, @Valid Set<String> aktørIdenter) { }


    public record PopulasjonInternRequest(@NotNull UUID ansattOid,
                                          @Valid Set<String> personIdenter,
                                          @Valid  Set<String> aktørIdenter) {

    }

    public record TilgangsvurderingDto(TilgangResultat tilgangResultat, @NotNull String årsak) {
    }

    private TilgangsvurderingDto mapTilVurderingDto(TilgangVurdering tilgangVurdering) {
        return new TilgangsvurderingDto(tilgangVurdering.tilgangResultat(), tilgangVurdering.årsak());
    }

    private static void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger et gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }

}
