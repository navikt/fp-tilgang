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
import no.nav.foreldrepenger.tilganger.domene.populasjon.SakTjeneste;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.PersondataPipDto;
import no.nav.vedtak.felles.integrasjon.ruting.RutingDto;
import no.nav.vedtak.felles.integrasjon.ruting.RutingResultat;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 * Kontrakter i fp-felles / tilgang-klient
 */
@ApplicationScoped
@Consumes(APPLICATION_JSON)
@Path("/ruting")
public class RutingRestTjeneste {

    private PopulasjonCache populasjonCache;
    private SakTjeneste sakTjeneste;

    RutingRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public RutingRestTjeneste(PopulasjonCache populasjonCache, SakTjeneste sakTjeneste) {
        this.populasjonCache = populasjonCache;
        this.sakTjeneste = sakTjeneste;
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/identer")
    public RutingDto.Respons finnRutingEgenskaperIdenter(@NotNull @Valid RutingDto.IdenterRequest request) {
        validerSystemKontekst();
        var pdlPips = populasjonCache.finnPdlPipFor(Optional.ofNullable(request.identer()).orElseGet(Set::of));
        var erNoenSkjermet = populasjonCache.finnSkjermingFor(pdlPips);
        return mapTilRespons(pdlPips, erNoenSkjermet);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/sak")
    public RutingDto.Respons finnRutingEgenskaperSak(@NotNull @Valid RutingDto.SakRequest request) {
        validerSystemKontekst();
        var identer = sakTjeneste.identerForSak(request.saksnummer());
        var pdlPips = populasjonCache.finnPdlPipFor(identer);
        var erNoenSkjermet = populasjonCache.finnSkjermingFor(pdlPips);
        return mapTilRespons(pdlPips, erNoenSkjermet);
    }

    private RutingDto.Respons mapTilRespons(Collection<PersondataPipDto> personPips, boolean skjerming) {
        Set<RutingResultat> resultat = new LinkedHashSet<>();
        if (personPips.stream().anyMatch(PersondataPipDto::harStrengAdresseBeskyttelse)) {
            resultat.add(RutingResultat.STRENGTFORTROLIG);
        }
        if (personPips.stream().anyMatch(PersondataPipDto::harFortroligAdresseBeskyttelse)) {
            resultat.add(RutingResultat.FORTROLIG);
        }
        if (skjerming) {
            resultat.add(RutingResultat.SKJERMING);
        }
        if (personPips.size() == 1 && personPips.stream().anyMatch(PersondataPipDto::harIkkeNasjonalTilknytning)) {
            resultat.add(RutingResultat.UTLAND);
        }
        return new RutingDto.Respons(resultat);
    }

    private static void validerSystemKontekst() {
        Objects.requireNonNull(KontekstHolder.getKontekst());
        if (!KontekstHolder.getKontekst().getIdentType().erSystem()) {
            throw new WebApplicationException("Trenger et gyldig CC token.", Response.Status.FORBIDDEN);
        }
    }

}
