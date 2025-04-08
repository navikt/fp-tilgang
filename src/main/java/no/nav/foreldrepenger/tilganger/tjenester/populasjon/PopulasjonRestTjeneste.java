package no.nav.foreldrepenger.tilganger.tjenester.populasjon;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonValue;

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
        var vurdering = populasjonTjeneste.vurderInternBruker(request.ansattOid(), Optional.ofNullable(request.personIdenter()).orElseGet(Set::of),
            Optional.ofNullable(request.aktørIdenter()).orElseGet(Set::of), request.saksnummer());
        return mapTilVurderingDto(vurdering);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/eksternbruker")
    public TilgangsvurderingDto sjekkEksternBruker(@NotNull @Valid PopulasjonRestTjeneste.PopulasjonEksternRequest request) {
        validerSystemKontekst();
        var vurdering = populasjonTjeneste.vurderEksternBruker(request.subjectPersonIdent(), request.aldersgrense(),
            Optional.ofNullable(request.personIdenter()).orElseGet(Set::of), Optional.ofNullable(request.aktørIdenter()).orElseGet(Set::of));
        return mapTilVurderingDto(vurdering);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/filtersaksnummer")
    public FilterResponse filterSaksnummer(@NotNull @Valid PopulasjonRestTjeneste.FilterSaksnummerRequest request) {
        validerSystemKontekst();
        Set<FilterReferanse> harTilgang = new LinkedHashSet<>();
        Set<FilterReferanse> ikkeTilgang = new LinkedHashSet<>();
        populasjonTjeneste.prefetchSaker(Optional.ofNullable(request.saker()).orElseGet(Map::of).values());
        Optional.ofNullable(request.saker()).orElseGet(Map::of).forEach((key, value) -> {
            var vurdering = populasjonTjeneste.vurderInternBruker(request.ansattOid(), Set.of(), value);
            leggTilFraVurdering(key, vurdering, harTilgang, ikkeTilgang);
        });
        return new FilterResponse(harTilgang, ikkeTilgang);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/filteridenter")
    public FilterResponse filterIdenter(@NotNull @Valid PopulasjonRestTjeneste.FilterIdenterRequest request) {
        validerSystemKontekst();
        Set<FilterReferanse> harTilgang = new LinkedHashSet<>();
        Set<FilterReferanse> ikkeTilgang = new LinkedHashSet<>();
        var identer = Optional.ofNullable(request.identer()).orElseGet(Map::of).values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        populasjonTjeneste.preFetchIdenter(identer);
        Optional.ofNullable(request.identer()).orElseGet(Map::of).forEach((key, value) -> {
            var vurdering = populasjonTjeneste.vurderInternBruker(request.ansattOid(), Optional.ofNullable(value).orElseGet(Set::of), null);
            leggTilFraVurdering(key, vurdering, harTilgang, ikkeTilgang);
        });
        return new FilterResponse(harTilgang, ikkeTilgang);
    }

    // Kun behov dersom man tar i bruk Redis og langvarig sakscache (mer enn 24 timer). Nå er det flere noder og Rest/Kafka når bare en av dem
    @POST
    @Produces(APPLICATION_JSON)
    @Path("/invalidersak")
    public Response invaliderSak(@NotNull @Valid PopulasjonRestTjeneste.SakRequest request) {
        validerSystemKontekst();
        populasjonTjeneste.invaliderSak(request.saksnummer());
        return Response.ok().build();
    }

    private void leggTilFraVurdering(FilterReferanse key, TilgangVurdering vurdering, Set<FilterReferanse> harTilgang, Set<FilterReferanse> ikkeTilgang) {
        if (vurdering.fikkTilgang()) {
            harTilgang.add(key);
        } else {
            ikkeTilgang.add(key);
        }
    }

    public record PopulasjonEksternRequest(@NotNull String subjectPersonIdent,
                                           @Valid Set<String> personIdenter,
                                           @Valid Set<String> aktørIdenter,
                                           @Valid Integer aldersgrense) { }


    public record PopulasjonInternRequest(@NotNull UUID ansattOid,
                                          @Valid Set<String> personIdenter,
                                          @Valid  Set<String> aktørIdenter,
                                          @Valid String saksnummer) { }

    public record SakRequest(@NotNull @Valid String saksnummer) { }


    public record TilgangsvurderingDto(TilgangResultat tilgangResultat, @NotNull String årsak) {
    }

    // For å sjekke hvilke nøkler den ansatte har tilgang til basert på saksnummer
    public record FilterSaksnummerRequest(@NotNull UUID ansattOid, Map<@Valid FilterReferanse, String> saker) { }

    // For å sjekke hvilke nøkler den ansatte har tilgang til basert på identer
    public record FilterIdenterRequest(@NotNull UUID ansattOid, Map<@Valid FilterReferanse, Set<String>> identer) { }

    // Hvilke av nøklene i request som den ansatte har tilgang til
    public record FilterResponse(Set<FilterReferanse> harTilgang, Set<FilterReferanse> ikkeTilgang) {}

    public record FilterReferanse(@JsonValue @NotNull @Size(max = 99) @Pattern(regexp = "^[a-zA-Z0-9_\\-]*$") String referanse) {

        public FilterReferanse {
            Objects.requireNonNull(referanse, "Referanse kan ikke være null");
        }
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
