package no.nav.foreldrepenger.tilganger.domene.populasjon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.tilganger.integrasjoner.pip.PdlPipKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.SkjermingPipKlient;
import no.nav.vedtak.felles.integrasjon.pdlpip.PersondataPipDto;
import no.nav.vedtak.util.LRUCache;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
public class PopulasjonCache {

    // TODO: Vurder Redis + CacheLifeTime - nå er den satt litt lang pga sammenlign/logg
    private static final long PERSON_CACHE_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS);

    private static final LRUCache<String, PersondataPipDto> PERSON_PIP = new LRUCache<>(30000, PERSON_CACHE_LIVE_TIME_MS);
    private static final LRUCache<String, Boolean> PERSON_SKJERMING = new LRUCache<>(15000, PERSON_CACHE_LIVE_TIME_MS);

    private PdlPipKlient pdlPipKlient;
    private SkjermingPipKlient skjermingPipKlient;

    PopulasjonCache() {
        // CDI proxy
    }

    @Inject
    public PopulasjonCache(PdlPipKlient pdlPipKlient, SkjermingPipKlient skjermingPipKlient) {
        this.pdlPipKlient = pdlPipKlient;
        this.skjermingPipKlient = skjermingPipKlient;
    }

    public Collection<PersondataPipDto> finnPdlPipFor(Collection<String> personIdenter, Collection<String> aktørIdenter) {
        var alleIdenter = new LinkedHashSet<>(aktørIdenter);
        alleIdenter.addAll(personIdenter);
        // Cached
        var harPdlPipForIdenter = alleIdenter.stream().map(PERSON_PIP::get).filter(Objects::nonNull).toList();
        if (harPdlPipForIdenter.size() == alleIdenter.size()) {
            return harPdlPipForIdenter;
        } else {
            // Hent de manglende fra PDL-PIP
            var hentetPdlPipForIdenter = alleIdenter.stream()
                .filter(i -> PERSON_PIP.get(i) == null)
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::hentPdlPipForIdenter));
            var pdlPips = new ArrayList<>(harPdlPipForIdenter);
            pdlPips.addAll(hentetPdlPipForIdenter);
            return pdlPips;
        }
    }

    public PersondataPipDto finnPdlPipFor(String ident) {
        return Optional.ofNullable(PERSON_PIP.get(ident))
            .orElseGet(() -> {
                var pip = pdlPipKlient.hentTilgangPersondata(ident);
                cachePersonPip(ident, pip);
                return pip;
            });
    }

    public boolean finnSkjermingFor(Collection<PersondataPipDto> persondataPipDtos) {
        // Map til PersonIdent
        var sjekkPersonIdenter = persondataPipDtos.stream()
            .map(this::finnPersonIdenter)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        // Skjekk blant cached verdier
        var skjermetForCachedIdenter = sjekkPersonIdenter.stream().map(PERSON_SKJERMING::get).filter(Objects::nonNull).anyMatch(b -> b);
        // Sjekk med henting av evt mangler
        return skjermetForCachedIdenter // eller hent og sjekk de som ikke finnes i cache - om noen
            || hentSkjermingFor(sjekkPersonIdenter.stream().filter(i -> PERSON_SKJERMING.get(i) == null).toList());
    }

    private Collection<PersondataPipDto> hentPdlPipForIdenter(List<String> identer) {
        if (identer.isEmpty()) {
            return List.of();
        }
        var pips = pdlPipKlient.hentTilgangPersondataBolk(identer);
        pips.forEach(this::cachePersonPip);
        return pips.values();
    }

    private boolean hentSkjermingFor(List<String> personIdenter) {
        if (personIdenter.isEmpty()) {
            return false;
        } else { // Todo bruk kommende ny tjeneste erSkjermet(List) fra denne klienten
            var erNoenSkjermet = skjermingPipKlient.erNoenSkjermet(personIdenter);
            personIdenter.forEach(pi -> PERSON_SKJERMING.put(pi, erNoenSkjermet));
            return erNoenSkjermet;
        }
    }

    private Collection<String> finnPersonIdenter(PersondataPipDto pipDto) {
        return Optional.ofNullable(pipDto.identer())
            .map(PersondataPipDto.Identer::identer).orElseGet(List::of).stream()
            .filter(i -> PersondataPipDto.IdentGruppe.FOLKEREGISTERIDENT.equals(i.gruppe()))
            .filter(i -> !i.historisk())
            .map(PersondataPipDto.Ident::ident)
            .collect(Collectors.toSet());
    }

    private void cachePersonPip(String ident, PersondataPipDto pip) {
        PERSON_PIP.put(ident, pip);
        PERSON_PIP.put(pip.aktoerId(), pip);
        pip.identer().identer().stream().filter(i -> !i.historisk()).forEach(i -> PERSON_PIP.put(i.ident(), pip));
    }

}
