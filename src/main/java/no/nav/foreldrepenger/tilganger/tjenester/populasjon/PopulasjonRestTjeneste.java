package no.nav.foreldrepenger.tilganger.tjenester.populasjon;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        var alleIdenter = new LinkedHashSet<>(Optional.ofNullable(request.identer()).orElseGet(Set::of));
        alleIdenter.addAll(Optional.ofNullable(request.personIdenter()).orElseGet(Set::of));
        alleIdenter.addAll(Optional.ofNullable(request.aktørIdenter()).orElseGet(Set::of));
        var saksnummer = request.saksnummer();
        try {
            saksnummer = populasjonTjeneste.validerBehandling(request.behandling(), request.saksnummer());
        } catch (Exception e) {
            return mapTilVurderingDto(TilgangVurdering.avslåGenerell("behandling matcher ikke sak"), null);
        }
        var vurdering = populasjonTjeneste.vurderInternBruker(request.ansattOid(), alleIdenter, saksnummer);
        var auditIdent = populasjonTjeneste.utledInternAuditIdent(alleIdenter, saksnummer);
        return mapTilVurderingDto(vurdering, auditIdent);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/eksternbruker")
    public TilgangsvurderingDto sjekkEksternBruker(@NotNull @Valid PopulasjonRestTjeneste.PopulasjonEksternRequest request) {
        validerSystemKontekst();
        var alleIdenter = new LinkedHashSet<>(Optional.ofNullable(request.identer()).orElseGet(Set::of));
        alleIdenter.addAll(Optional.ofNullable(request.personIdenter()).orElseGet(Set::of));
        alleIdenter.addAll(Optional.ofNullable(request.aktørIdenter()).orElseGet(Set::of));
        var vurdering = populasjonTjeneste.vurderEksternBruker(request.subjectPersonIdent(), request.aldersgrense(), alleIdenter);
        var auditIdent = populasjonTjeneste.utledEksternAuditIdent(alleIdenter, request.subjectPersonIdent());
        return mapTilVurderingDto(vurdering, auditIdent);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/filtersaksnummer")
    public FilterResponse filterSaksnummer(@NotNull @Valid PopulasjonRestTjeneste.FilterSaksnummerRequest request) {
        validerSystemKontekst();
        var requestSaker = Optional.ofNullable(request.saker()).orElseGet(Set::of);
        populasjonTjeneste.prefetchSaker(requestSaker);
        var filtrertSaker = requestSaker.stream()
            .filter(s -> populasjonTjeneste.vurderInternBruker(request.ansattOid(), Set.of(), s).fikkTilgang())
            .collect(Collectors.toSet());
        return new FilterResponse(filtrertSaker);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/filteridenter")
    public FilterResponse filterIdenter(@NotNull @Valid PopulasjonRestTjeneste.FilterIdenterRequest request) {
        validerSystemKontekst();
        var requestIdenter = Optional.ofNullable(request.identer()).orElseGet(Set::of);
        populasjonTjeneste.preFetchIdenter(requestIdenter);
        var filtrertIdenter = requestIdenter.stream()
            .filter(i -> populasjonTjeneste.vurderInternBruker(request.ansattOid(), Set.of(i), null).fikkTilgang())
            .collect(Collectors.toSet());
        return new FilterResponse(filtrertIdenter);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/invalidersak")
    public Response invaliderSak(@NotNull @Valid PopulasjonRestTjeneste.SakRequest request) {
        validerSystemKontekst();
        populasjonTjeneste.invaliderSak(request.saksnummer());
        return Response.ok().build();
    }

    public record PopulasjonEksternRequest(@NotNull String subjectPersonIdent,
                                           @Valid Set<String> personIdenter,
                                           @Valid Set<String> aktørIdenter,
                                           @Valid Set<String> identer,
                                           @Valid Integer aldersgrense) { }


    public record PopulasjonInternRequest(@NotNull UUID ansattOid,
                                          @Valid Set<String> personIdenter,
                                          @Valid Set<String> aktørIdenter,
                                          @Valid Set<String> identer,
                                          @Valid String saksnummer,
                                          @Valid UUID behandling) { }

    public record SakRequest(@NotNull @Valid String saksnummer) { }


    public record TilgangsvurderingDto(TilgangResultat tilgangResultat, @NotNull String årsak, String auditIdent) {
    }

    // For å sjekke hvilke saker den ansatte har tilgang til basert på saksnummer
    public record FilterSaksnummerRequest(@NotNull UUID ansattOid, Set<String> saker) { }

    // For å sjekke hvilke identer den ansatte har tilgang til basert på identer
    public record FilterIdenterRequest(@NotNull UUID ansattOid, Set<String> identer) { }

    // Hvilke av sakene/identene i request som den ansatte har tilgang til
    public record FilterResponse(Set<String> harTilgang) {}


    private TilgangsvurderingDto mapTilVurderingDto(TilgangVurdering tilgangVurdering, String auditIdent) {
        return new TilgangsvurderingDto(tilgangVurdering.tilgangResultat(), tilgangVurdering.årsak(), auditIdent);
    }

    protected void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger et gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }

}
