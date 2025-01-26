package no.nav.foreldrepenger.tilganger.domene.populasjon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.tilganger.domene.ansatt.AnsattTjeneste;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.PdlPipKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.SkjermingPipKlient;
import no.nav.vedtak.felles.integrasjon.pdlpip.PersondataPipDto;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppeProvider;
import no.nav.vedtak.util.LRUCache;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
public class PopulasjonTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS);

    private static final LRUCache<String, PersondataPipDto> PERSON_PIP = new LRUCache<>(30000, CACHE_ELEMENT_LIVE_TIME_MS);
    private static final LRUCache<String, Boolean> PERSON_SKJERMING = new LRUCache<>(15000, CACHE_ELEMENT_LIVE_TIME_MS);

    private static final AnsattGruppeProvider PROVIDER = AnsattGruppeProvider.instance();
    private static final Set<UUID> ALLE_ANSATTGRUPPE_OIDS = Arrays.stream(AnsattGruppe.values())
        .map(PROVIDER::getAnsattGruppeOid)
        .collect(Collectors.toSet());

    private AnsattTjeneste ansattTjeneste;
    private PdlPipKlient pdlPipKlient;
    private SkjermingPipKlient skjermingPipKlient;

    PopulasjonTjeneste() {
        // CDI proxy
    }

    @Inject
    public PopulasjonTjeneste(AnsattTjeneste ansattTjeneste, PdlPipKlient pdlPipKlient, SkjermingPipKlient skjermingPipKlient) {
        this.ansattTjeneste = ansattTjeneste;
        this.pdlPipKlient = pdlPipKlient;
        this.skjermingPipKlient = skjermingPipKlient;
    }


    public TilgangVurdering vurderInternBruker(UUID ansattOID, Set<AnsattGruppe> kreverGrupper,
                                               Set<String> personIdenter, Set<String> aktørIdenter) {
        var sjekkGrupper = new HashSet<>(kreverGrupper);
        if (!personIdenter.isEmpty() || !aktørIdenter.isEmpty()) {
            // Finn behov for ekstra AD-grupper for tilfelle av adressebeskyttelse eller skjerming
            var allePersonPips = finnPdlPipFor(personIdenter, aktørIdenter);
            sjekkAdresseBeskyttelse(allePersonPips).ifPresent(sjekkGrupper::add);
            sjekkSkjerming(allePersonPips).ifPresent(sjekkGrupper::add);
        }
        if (sjekkGrupper.isEmpty()) {
            return TilgangVurdering.godkjenn();
        }
        var gruppeOids = ansattTjeneste.hentGrupper(ansattOID, ALLE_ANSATTGRUPPE_OIDS);
        var harAnsattGrupper = PROVIDER.getAnsattGrupperFra(gruppeOids);
        if (sjekkGrupper.contains(AnsattGruppe.STRENGTFORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.STRENGTFORTROLIG)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_KODE_6);
        }
        if (sjekkGrupper.contains(AnsattGruppe.FORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.FORTROLIG)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_KODE_7);
        }
        if (sjekkGrupper.contains(AnsattGruppe.SKJERMET) && !harAnsattGrupper.contains(AnsattGruppe.SKJERMET)) {
            return TilgangVurdering.avslå(TilgangResultat.AVSLÅTT_EGEN_ANSATT);
        }
        var manglende = sjekkGrupper.stream().filter(g -> !harAnsattGrupper.contains(g)).toList();
        if (!manglende.isEmpty()) {
            return TilgangVurdering.avslåGenerell("Ikke med i grupper " + manglende);
        }
        return TilgangVurdering.godkjenn();
    }

    public TilgangVurdering vurderEksternBruker(String subjectPersonIdent, Set<String> personIdenter, Set<String> aktørIdenter) {
        var subjectPip = Optional.ofNullable(PERSON_PIP.get(subjectPersonIdent)).orElseGet(() -> hentPdlPipFor(subjectPersonIdent));
        if (subjectPip.erIkkeMyndig()) {
            return TilgangVurdering.avslåGenerell("Ikke gammel nok");
        }
        var subjectAktørId = subjectPip.aktørId();
        if (personIdenter.size() == 1 && personIdenter.stream().anyMatch(pi -> Objects.equals(pi, subjectPersonIdent))) {
            return TilgangVurdering.godkjenn();
        }
        if (aktørIdenter.size() == 1 && aktørIdenter.stream().anyMatch(ai -> Objects.equals(ai, subjectAktørId))) {
            return TilgangVurdering.godkjenn();
        }
        return TilgangVurdering.avslåGenerell("Har bare tilgang til seg selv");
    }

    private Optional<AnsattGruppe> sjekkAdresseBeskyttelse(Collection<PersondataPipDto> persondataPipDtos) {
        if (persondataPipDtos.stream().anyMatch(PersondataPipDto::harStrengAdresseBeskyttelse)) {
            return Optional.of(AnsattGruppe.STRENGTFORTROLIG);
        } else if (persondataPipDtos.stream().anyMatch(PersondataPipDto::harAdresseBeskyttelse)) {
            return Optional.of(AnsattGruppe.FORTROLIG);
        } else {
            return Optional.empty();
        }
    }

    private Optional<AnsattGruppe> sjekkSkjerming(Collection<PersondataPipDto> persondataPipDtos) {
        // Map til PersonIdent
        var sjekkPersonIdenter = persondataPipDtos.stream()
            .map(this::finnPersonIdenter)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        // Skjekk blant cached verdier + refresh cache
        sjekkPersonIdenter.stream().filter(i -> PERSON_SKJERMING.get(i) != null).forEach(i -> PERSON_SKJERMING.put(i, PERSON_SKJERMING.get(i)));
        var skjermetForCachedIdenter = sjekkPersonIdenter.stream().map(PERSON_SKJERMING::get).filter(Objects::nonNull).anyMatch(b -> b);
        // Hvilke mangler
        var hentSkjermingForIdenter = sjekkPersonIdenter.stream().filter(i -> PERSON_SKJERMING.get(i) == null).toList();
        // Sjekk med henting av evt mangler
        if (skjermetForCachedIdenter || hentSkjermingFor(hentSkjermingForIdenter)) {
            return Optional.of(AnsattGruppe.SKJERMET);
        } else {
            return Optional.empty();
        }
    }

    private Collection<PersondataPipDto> finnPdlPipFor(Collection<String> personIdenter, Collection<String> aktørIdenter) {
        var alleIdenter = new LinkedHashSet<>(aktørIdenter);
        alleIdenter.addAll(personIdenter);
        // Cached
        var harPdlPipForIdenter = alleIdenter.stream().map(PERSON_PIP::get).filter(Objects::nonNull).toList();
        // Hent de manglende fra PDL-PIP
        var hentetPdlPipForIdenter = alleIdenter.stream()
            .filter(i -> PERSON_PIP.get(i) == null)
            .collect(Collectors.collectingAndThen(Collectors.toList(), this::hentPdlPipForIdenter));
        var pdlPips = new ArrayList<>(harPdlPipForIdenter);
        pdlPips.addAll(hentetPdlPipForIdenter);
        // Legg til eller forleng cache-levetid basert på identer i PersonPip
        pdlPips.forEach(pip -> PERSON_PIP.put(pip.aktoerId(), pip));
        pdlPips.forEach(pip -> finnIdenter(pip).forEach(i -> PERSON_PIP.put(i.ident(), pip)));
        return pdlPips;
    }

    private Collection<PersondataPipDto> hentPdlPipForIdenter(List<String> identer) {
        if (identer.isEmpty()) return List.of();
        var pips = pdlPipKlient.hentTilgangPersondataBolk(identer);
        pips.forEach(PERSON_PIP::put);
        return pips.values();
    }

    private PersondataPipDto hentPdlPipFor(String ident) {
        var pip = pdlPipKlient.hentTilgangPersondata(ident);
        PERSON_PIP.put(ident, pip);
        PERSON_PIP.put(pip.aktoerId(), pip);
        pip.identer().identer().stream().filter(i -> !i.historisk()).forEach(i -> PERSON_PIP.put(i.ident(), pip));
        return pip;
    }

    private boolean hentSkjermingFor(List<String> personIdenter) {
        if (personIdenter.isEmpty()) return false;
        var erNoenSkjermet = skjermingPipKlient.erNoenSkjermet(personIdenter);
        personIdenter.forEach(pi -> PERSON_SKJERMING.put(pi, erNoenSkjermet));
        return erNoenSkjermet;
    }

    private Collection<PersondataPipDto.Ident> finnIdenter(PersondataPipDto pipDto) {
        return Optional.ofNullable(pipDto.identer())
            .map(PersondataPipDto.Identer::identer).orElseGet(List::of).stream()
            .filter(i -> !i.historisk())
            .collect(Collectors.toSet());
    }

    private Collection<String> finnPersonIdenter(PersondataPipDto pipDto) {
        return Optional.ofNullable(pipDto.identer())
            .map(PersondataPipDto.Identer::identer).orElseGet(List::of).stream()
            .filter(i -> PersondataPipDto.IdentGruppe.FOLKEREGISTERIDENT.equals(i.gruppe()))
            .filter(i -> !i.historisk())
            .map(PersondataPipDto.Ident::ident)
            .collect(Collectors.toSet());
    }

}
