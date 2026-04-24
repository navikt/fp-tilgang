package no.nav.foreldrepenger.tilganger.domene.populasjon;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.tilganger.domene.ansatt.GrupperTjeneste;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.PersondataPipDto;
import no.nav.vedtak.felles.integrasjon.populasjon.PopulasjonTilgangResultat;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
public class PopulasjonTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PopulasjonTjeneste.class);

    private static final Integer DEFAULT_ALDERSGRENSE = 18;

    private PopulasjonCache populasjonCache;
    private SakTjeneste sakTjeneste;
    private GrupperTjeneste grupperTjeneste;

    PopulasjonTjeneste() {
        // CDI proxy
    }

    @Inject
    public PopulasjonTjeneste(PopulasjonCache populasjonCache,
                              SakTjeneste sakTjeneste,
                              GrupperTjeneste grupperTjeneste) {
        this.populasjonCache = populasjonCache;
        this.sakTjeneste = sakTjeneste;
        this.grupperTjeneste = grupperTjeneste;
    }


    public TilgangVurdering vurderInternBruker(UUID ansattOID, Set<String> identer, String saksnummer) {
        if (saksnummer != null) {
            var alleIdenter = new LinkedHashSet<>(sakTjeneste.identerForSak(saksnummer));
            alleIdenter.addAll(identer);
            return vurderInternBruker(ansattOID, alleIdenter);
        } else {
            return vurderInternBruker(ansattOID, identer);
        }
    }

    private TilgangVurdering vurderInternBruker(UUID ansattOID, Set<String> identer) {
        Set<AnsattGruppe> nødvendigeGrupper = new HashSet<>();

        // Finn behov for ekstra AD-grupper for tilfelle av adressebeskyttelse eller skjerming
        var allePersonPips = populasjonCache.finnPdlPipFor(identer);
        if (allePersonPips.stream().anyMatch(PersondataPipDto::harStrengAdresseBeskyttelse)) {
            nødvendigeGrupper.add(AnsattGruppe.STRENGTFORTROLIG);
        } else if (allePersonPips.stream().anyMatch(PersondataPipDto::harAdresseBeskyttelse)) {
            nødvendigeGrupper.add(AnsattGruppe.FORTROLIG);
        }
        if (populasjonCache.finnSkjermingFor(allePersonPips)) {
           nødvendigeGrupper.add(AnsattGruppe.SKJERMET);
        }

        // Tidlig godkjenning dersom det ikke er behov for særskilte AD-grupper, ellers hent den ansattes grupper
        if (nødvendigeGrupper.isEmpty()) {
            return TilgangVurdering.godkjenn();
        }
        var harAnsattGrupper = grupperTjeneste.alleGrupperForAnsatt(ansattOID);

        // Sjekk om den ansatte er med i nødvendige grupper
        if (nødvendigeGrupper.contains(AnsattGruppe.STRENGTFORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.STRENGTFORTROLIG)) {
            return TilgangVurdering.avslå(PopulasjonTilgangResultat.AVSLÅTT_KODE_6);
        } else if (nødvendigeGrupper.contains(AnsattGruppe.FORTROLIG) && !harAnsattGrupper.contains(AnsattGruppe.FORTROLIG)) {
            return TilgangVurdering.avslå(PopulasjonTilgangResultat.AVSLÅTT_KODE_7);
        } else if (nødvendigeGrupper.contains(AnsattGruppe.SKJERMET) && !harAnsattGrupper.contains(AnsattGruppe.SKJERMET)) {
            return TilgangVurdering.avslå(PopulasjonTilgangResultat.AVSLÅTT_EGEN_ANSATT);
        } else {
            return TilgangVurdering.godkjenn();
        }
    }

    public TilgangVurdering vurderEksternBruker(String subjectPersonIdent, Integer aldersgrense, Set<String> identer) {
        var subjectPipOpt = populasjonCache.finnPdlPipFor(Set.of(subjectPersonIdent)).stream().findFirst();
        if (subjectPipOpt.isEmpty()) {
            return TilgangVurdering.avslåGenerell("Finner ikke innlogget bruker");
        }
        var subjectPip = subjectPipOpt.get();
        if (subjectPip.erUnderAlder(Optional.ofNullable(aldersgrense).orElse(DEFAULT_ALDERSGRENSE))) {
            return TilgangVurdering.avslåGenerell("Ikke gammel nok");
        }
        var subjectAktørId = subjectPip.aktørId();
        if (identer.size() == 1 && identer.stream().anyMatch(pi -> Objects.equals(pi, subjectPersonIdent))) {
            return TilgangVurdering.godkjenn();
        }
        if (identer.size() == 1 && identer.stream().anyMatch(ai -> Objects.equals(ai, subjectAktørId))) {
            return TilgangVurdering.godkjenn();
        }
        return TilgangVurdering.avslåGenerell("Har bare tilgang til seg selv");
    }

    /**
     * Validerer behandlingUuid
     */
    public String validerBehandling(UUID behandling, String saksnummer) {
        if (behandling == null) {
            return saksnummer;
        }
        var sakForBehandling = sakTjeneste.saksnummerForBehandling(behandling);
        if (saksnummer != null && sakForBehandling.isPresent() && !saksnummer.equals(sakForBehandling.get())) {
            throw new IllegalArgumentException("Behandling " + behandling + " tilhører ikke oppgitt sak " + saksnummer);
        }
        return sakForBehandling.orElse(saksnummer);
    }

    /**
     * Ident til audit-logging
     */
    public String utledInternAuditIdent(Set<String> identer, String saksnummer) {
        return Optional.ofNullable(saksnummer).flatMap(sakTjeneste::saksidentForSak)
            .or(() -> identer.stream().findFirst())
            .flatMap(populasjonCache::finnPersonIdentFor).orElse(null);
    }

    public String utledEksternAuditIdent(Set<String> identer, String subjectPersonIdent) {
        return identer.stream().findFirst().flatMap(populasjonCache::finnPersonIdentFor).orElse(subjectPersonIdent);
    }

    /**
     * Tjenester for utgående filtrering av resultater
     */
    public void preFetchIdenter(Collection<String> identer) {
        var pips = populasjonCache.finnPdlPipFor(identer);
        populasjonCache.finnSkjermingFor(pips);
    }

    public void prefetchSaker(Collection<String> saksnummer) {
        var identer = sakTjeneste.prefetchSaker(saksnummer);
        preFetchIdenter(identer);
    }

    /**
     * Sak er endret med tanke på persongalleri
     */
    public void invaliderSak(String saksnummer) {
        LOG.info("Invalider sak {}", saksnummer);
        sakTjeneste.invaliderSak(saksnummer);
    }
}
