package no.nav.foreldrepenger.tilganger.domene.populasjon;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.tilganger.domene.cache.SakCache;
import no.nav.foreldrepenger.tilganger.integrasjoner.pip.FpsakPipKlient;

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
        var identer = sakCache.read(saksnummer);
        if (identer.isEmpty()) {
            var før = System.currentTimeMillis();
            identer = new HashSet<>(fpsakPipKlient.personerForSak(saksnummer));
            sakCache.store(saksnummer, identer);
            LOG.info("[{} ms] Hent sakIdenter.", System.currentTimeMillis() - før);
        }
        return identer;
    }

    public Set<String> prefetchSaker(Collection<String> saksnummer) {
        var manglende = saksnummer.stream().filter(s -> sakCache.read(s).isEmpty()).collect(Collectors.toSet());
        if (!manglende.isEmpty()) {
            var før = System.currentTimeMillis();
            var hentet = fpsakPipKlient.personerForSaker(manglende);
            hentet.forEach(s -> sakCache.store(s.saksnummer(), s.identer()));
            LOG.info("[{} ms] Hent prefetch saker manglet {} hentet {}.", System.currentTimeMillis() - før, manglende.size(), hentet.size());
        }
        return saksnummer.stream().map(sakCache::read).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public void invaliderSak(String saksnummer) {
        LOG.info("Invalider sak {}", saksnummer);
        sakCache.remove(saksnummer);
    }
}
