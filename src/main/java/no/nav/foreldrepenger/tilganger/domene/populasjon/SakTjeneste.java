package no.nav.foreldrepenger.tilganger.domene.populasjon;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.tilganger.domene.cache.SakCache;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.FpsakPipKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.SakMedPersonerDto;

/**
 * Brukes av applikasjoner som skal gjøre tilgangskontroll for internbruker eller eksternbruker
 */
@ApplicationScoped
public class SakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SakTjeneste.class);

    private SakCache sakCache;
    private FpsakPipKlient fpsakPipKlient;

    SakTjeneste() {
        // CDI proxy
    }

    @Inject
    public SakTjeneste(SakCache sakCache, FpsakPipKlient fpsakPipKlient) {
        this.sakCache = sakCache;
        this.fpsakPipKlient = fpsakPipKlient;
    }

    public Collection<String> identerForSak(String saksnummer) {
        var identer = sakCache.readIdenter(saksnummer);
        if (identer.isEmpty()) {
            var før = System.currentTimeMillis();
            var sakFullInfo = fpsakPipKlient.personerForSak(saksnummer);
            identer = new HashSet<>(sakFullInfo.map(SakMedPersonerDto::identer).orElseGet(Set::of));
            sakCache.storeIdenter(saksnummer, identer);
            sakFullInfo.map(SakMedPersonerDto::saksident).ifPresent(si -> sakCache.storeSaksident(saksnummer, si));
            LOG.info("[{} ms] Hent sakIdenter.", System.currentTimeMillis() - før);
        }
        return identer;
    }

    public Optional<String> saksidentForSak(String saksnummer) {
        var saksident = Optional.ofNullable(sakCache.readSaksident(saksnummer));
        if (saksident.isEmpty()) {
            var før = System.currentTimeMillis();
            saksident = fpsakPipKlient.sakIdentForSak(saksnummer);
            saksident.ifPresent(si -> sakCache.storeSaksident(saksnummer, si));
            LOG.info("[{} ms] Hent saksident.", System.currentTimeMillis() - før);
        }
        return saksident;
    }

    public Optional<String> saksnummerForBehandling(UUID behandling) {
        var saksnummer = Optional.ofNullable(sakCache.readBehandlingSaksnummer(behandling));
        if (saksnummer.isEmpty()) {
            var før = System.currentTimeMillis();
            saksnummer = fpsakPipKlient.saksnummerForBehandling(behandling);
            saksnummer.ifPresent(si -> sakCache.storeBehandlingSaksnummer(behandling, si));
            LOG.info("[{} ms] Hent saksnummer.", System.currentTimeMillis() - før);
        }
        return saksnummer;
    }

    public Set<String> prefetchSaker(Collection<String> saksnummer) {
        var manglende = saksnummer.stream().filter(s -> sakCache.readIdenter(s).isEmpty()).collect(Collectors.toSet());
        if (!manglende.isEmpty()) {
            var før = System.currentTimeMillis();
            var hentet = fpsakPipKlient.personerForSaker(manglende);
            hentet.forEach(s -> sakCache.storeIdenter(s.saksnummer(), s.identer()));
            hentet.forEach(s -> Optional.ofNullable(s.saksident()).ifPresent(si -> sakCache.storeSaksident(s.saksnummer(), si)));
            LOG.info("[{} ms] Hent prefetch saker manglet {} hentet {}.", System.currentTimeMillis() - før, manglende.size(), hentet.size());
        }
        return saksnummer.stream().map(sakCache::readIdenter).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public void invaliderSak(String saksnummer) {
        LOG.info("Invalider sak {}", saksnummer);
        sakCache.removeIdenter(saksnummer);
    }
}
