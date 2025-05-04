package no.nav.foreldrepenger.tilganger.tjenester.populasjon;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import no.nav.foreldrepenger.tilganger.domene.populasjon.TilgangVurdering;
import no.nav.vedtak.felles.integrasjon.populasjon.PopulasjonDto;
import no.nav.vedtak.felles.integrasjon.sak.InvaliderSakRequest;
import no.nav.vedtak.felles.integrasjon.tilgangfilter.FilterDto;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker, kalles fra fp-felles
 * Kontrakter i fp-felles / tilgang-klient
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
    public PopulasjonDto.Respons sjekkInternBruker(@NotNull @Valid PopulasjonDto.InternRequest request) {
        validerSystemKontekst();
        var alleIdenter = new LinkedHashSet<>(Optional.ofNullable(request.identer()).orElseGet(Set::of));
        var saksnummer = request.saksnummer();
        try {
            saksnummer = populasjonTjeneste.validerBehandling(request.behandling(), request.saksnummer());
        } catch (Exception e) {
            var auditIdent = populasjonTjeneste.utledInternAuditIdent(alleIdenter, saksnummer);
            return mapTilVurderingDto(TilgangVurdering.avslåGenerell("behandling matcher ikke sak"), auditIdent);
        }
        var vurdering = populasjonTjeneste.vurderInternBruker(request.ansattOid(), alleIdenter, saksnummer);
        var auditIdent = populasjonTjeneste.utledInternAuditIdent(alleIdenter, saksnummer);
        return mapTilVurderingDto(vurdering, auditIdent);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/eksternbruker")
    public PopulasjonDto.Respons sjekkEksternBruker(@NotNull @Valid PopulasjonDto.EksternRequest request) {
        validerSystemKontekst();
        var alleIdenter = new LinkedHashSet<>(Optional.ofNullable(request.identer()).orElseGet(Set::of));
        var vurdering = populasjonTjeneste.vurderEksternBruker(request.subjectPersonIdent(), request.aldersgrense(), alleIdenter);
        var auditIdent = populasjonTjeneste.utledEksternAuditIdent(alleIdenter, request.subjectPersonIdent());
        return mapTilVurderingDto(vurdering, auditIdent);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/filtersaksnummer")
    public FilterDto.Respons filterSaksnummer(@NotNull @Valid FilterDto.SaksnummerRequest request) {
        validerSystemKontekst();
        var requestSaker = Optional.ofNullable(request.saker()).orElseGet(Set::of);
        populasjonTjeneste.prefetchSaker(requestSaker);
        var filtrertSaker = requestSaker.stream()
            .filter(s -> populasjonTjeneste.vurderInternBruker(request.ansattOid(), Set.of(), s).fikkTilgang())
            .collect(Collectors.toSet());
        return new FilterDto.Respons(filtrertSaker);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/filteridenter")
    public FilterDto.Respons filterIdenter(@NotNull @Valid FilterDto.IdenterRequest request) {
        validerSystemKontekst();
        var requestIdenter = Optional.ofNullable(request.identer()).orElseGet(Set::of);
        populasjonTjeneste.preFetchIdenter(requestIdenter);
        var filtrertIdenter = requestIdenter.stream()
            .filter(i -> populasjonTjeneste.vurderInternBruker(request.ansattOid(), Set.of(i), null).fikkTilgang())
            .collect(Collectors.toSet());
        return new FilterDto.Respons(filtrertIdenter);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/invalidersak")
    public Response invaliderSak(@NotNull @Valid InvaliderSakRequest request) {
        validerSystemKontekst();
        populasjonTjeneste.invaliderSak(request.saksnummer());
        return Response.ok().build();
    }

    private PopulasjonDto.Respons mapTilVurderingDto(TilgangVurdering tilgangVurdering, String auditIdent) {
        return new PopulasjonDto.Respons(tilgangVurdering.tilgangResultat(), tilgangVurdering.årsak(), auditIdent);
    }

    protected void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger et gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }

}
