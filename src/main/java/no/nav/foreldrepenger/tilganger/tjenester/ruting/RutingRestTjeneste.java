package no.nav.foreldrepenger.tilganger.tjenester.ruting;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

import no.nav.foreldrepenger.tilganger.domene.populasjon.PopulasjonCache;
import no.nav.vedtak.felles.integrasjon.pdlpip.PersondataPipDto;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/ruting")
public class RutingRestTjeneste {

    private PopulasjonCache populasjonCache;

    RutingRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public RutingRestTjeneste(PopulasjonCache populasjonCache) {
        this.populasjonCache = populasjonCache;
    }


    @POST
    @Produces(APPLICATION_JSON)
    @Path("/egenskaper")
    public RutingResponsDto finnRutingEgenskaper(@NotNull @Valid RutingRestTjeneste.RutingRequest request) {
        validerSystemKontekst();
        var pdlPips = populasjonCache.finnPdlPipFor(Set.of(), Optional.ofNullable(request.aktørIdenter()).orElseGet(Set::of));
        var erNoenSkjermet = populasjonCache.finnSkjermingFor(pdlPips);
        return mapTilRespons(pdlPips, erNoenSkjermet);
    }


    public record RutingRequest(@Valid Set<String> aktørIdenter) { }

    public enum RutingResultat { STRENGTFORTROLIG, SKJERMING, UTLAND }

    public record RutingResponsDto(Set<RutingResultat> resultater) { }

    private RutingResponsDto mapTilRespons(Collection<PersondataPipDto> personPips, boolean skjerming) {
        Set<RutingResultat> resultat = new LinkedHashSet<>();
        if (personPips.stream().anyMatch(PersondataPipDto::harStrengAdresseBeskyttelse)) {
            resultat.add(RutingResultat.STRENGTFORTROLIG);
        }
        if (skjerming) {
            resultat.add(RutingResultat.SKJERMING);
        }
        if (personPips.size() == 1 && personPips.stream().anyMatch(PersondataPipDto::harIkkeNasjonalTilknytning)) {
            resultat.add(RutingResultat.UTLAND);
        }
        return new RutingResponsDto(resultat);
    }

    private static void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger et gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }

}
