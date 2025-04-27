package no.nav.foreldrepenger.tilganger.domene.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheUtilgjengeligException;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisDatabase;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class SakCache {

    private static final Logger LOG = LoggerFactory.getLogger(SakCache.class);
    private static final long CACHE_DURATION = Duration.ofDays(30).getSeconds(); // Persongalleri stabilt over tid, men saker behandles raskt
    static final String CACHE_KEY_IDENTER_PREFIX = "sak_";
    static final String CACHE_KEY_SAKSIDENT_PREFIX = "ident_";
    static final String CACHE_KEY_BEHANDLING_PREFIX = "behandling_";
    private static final String IDENT_INFIX = ":";
    static final RedisDatabase REDIS_SAK_CACHE = RedisDatabase.TWO;

    private final RedisCacheKlient redisCache;
    private final LRUCache<String, String> lokalCache;

    // Til testing
    SakCache(RedisCacheKlient redisCache, LRUCache<String, String> lokalCache) {
        this.redisCache = redisCache;
        this.lokalCache = lokalCache;
    }

    @Inject
    public SakCache() {
        this.redisCache = RedisCacheKlient.instance();
        this.lokalCache = new LRUCache<>(1500, CACHE_DURATION * 100); // LRUCache tar millisekunder, så blir 3 dager
    }

    public void storeIdenter(String key, Set<String> value) {
        var cacheKey = hentCacheKey(key);
        var joined = value != null ? String.join(IDENT_INFIX, value).trim() : "";
        try {
            LOG.debug("Redis storing identer for key '{}'", cacheKey);
            redisCache.lagre(cacheKey, joined, CACHE_DURATION, REDIS_SAK_CACHE);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.put(cacheKey, joined);
        }
    }

    public Set<String> readIdenter(String key) {
        var cacheKey = hentCacheKey(key);
        try {
            LOG.debug("Redis reading identer for key: {}", cacheKey);
            return redisCache.les(cacheKey, REDIS_SAK_CACHE).map(SakCache::splitStrings).orElseGet(Set::of);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
        }
        return Optional.ofNullable(lokalCache.get(cacheKey)).map(SakCache::splitStrings).orElseGet(Set::of);
    }

    private static Set<String> splitStrings(String joined) {
        return Arrays.stream(joined.split(IDENT_INFIX))
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());
    }

    public void removeIdenter(String key) {
        var cacheKey = hentCacheKey(key);
        try {
            var verdi = redisCache.les(cacheKey, REDIS_SAK_CACHE);
            if (verdi.isPresent()) {
                LOG.debug("Redis removing identer for key '{}'", cacheKey);
                redisCache.fjern(cacheKey, REDIS_SAK_CACHE);
            }
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.remove(cacheKey);
        }
    }

    public void storeSaksident(String key, String value) {
        var cacheKey = hentSaksidentCacheKey(key);
        try {
            LOG.debug("Redis storing saksident for key '{}'", cacheKey);
            redisCache.lagre(cacheKey, value, CACHE_DURATION, REDIS_SAK_CACHE);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.put(cacheKey, value);
        }
    }

    public String readSaksident(String key) {
        var cacheKey = hentSaksidentCacheKey(key);
        try {
            LOG.debug("Redis reading saksident for key: {}", cacheKey);
            return redisCache.les(cacheKey, REDIS_SAK_CACHE).orElse(null);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
        }
        return lokalCache.get(cacheKey);
    }

    public void removeSaksident(String key) {
        var cacheKey = hentSaksidentCacheKey(key);
        try {
            var verdi = redisCache.les(cacheKey, REDIS_SAK_CACHE);
            if (verdi.isPresent()) {
                LOG.debug("Redis removing saksident for key '{}'", cacheKey);
                redisCache.fjern(cacheKey, REDIS_SAK_CACHE);
            }
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.remove(cacheKey);
        }
    }

    public void storeBehandlingSaksnummer(UUID key, String value) {
        var cacheKey = hentBehandlingCacheKey(key);
        try {
            LOG.debug("Redis storing saksident for key '{}'", cacheKey);
            redisCache.lagre(cacheKey, value, CACHE_DURATION, REDIS_SAK_CACHE);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.put(cacheKey, value);
        }
    }

    public String readBehandlingSaksnummer(UUID key) {
        var cacheKey = hentBehandlingCacheKey(key);
        try {
            LOG.debug("Redis reading saksident for key: {}", cacheKey);
            return redisCache.les(cacheKey, REDIS_SAK_CACHE).orElse(null);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
        }
        return lokalCache.get(cacheKey);
    }

    public void removeBehandlingSaksnummer(UUID key) {
        var cacheKey = hentBehandlingCacheKey(key);
        try {
            var verdi = redisCache.les(cacheKey, REDIS_SAK_CACHE);
            if (verdi.isPresent()) {
                LOG.debug("Redis removing saksident for key '{}'", cacheKey);
                redisCache.fjern(cacheKey, REDIS_SAK_CACHE);
            }
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.remove(cacheKey);
        }
    }

    public void deleteCache() {
        LOG.info("Fjerner sak cache.");
        try {
            redisCache.slettCache(REDIS_SAK_CACHE);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
        }
    }

    static String hentCacheKey(String key) {
        return CACHE_KEY_IDENTER_PREFIX + key;
    }

    static String hentSaksidentCacheKey(String key) {
        return CACHE_KEY_SAKSIDENT_PREFIX + key;
    }

    static String hentBehandlingCacheKey(UUID key) {
        return CACHE_KEY_BEHANDLING_PREFIX + key.toString();
    }

    private static void logRedisUtilgjengelig() {
        LOG.info("Redis ikke tilgjengelig. Kjører videre med lokal cache.");
    }
}
