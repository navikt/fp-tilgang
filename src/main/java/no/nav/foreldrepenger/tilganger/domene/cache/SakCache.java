package no.nav.foreldrepenger.tilganger.domene.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheUtilgjengeligException;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisDatabase;
import no.nav.vedtak.util.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SakCache {

    private static final Logger LOG = LoggerFactory.getLogger(SakCache.class);
    private static final long CACHE_DURATION = Duration.ofDays(10).getSeconds(); // Persongalleri stabilt over tid, men saker behandles raskt
    static final String CACHE_KEY_PREFIX = "sak_";
    private static final String IDENT_INFIX = ":";
    static final RedisDatabase REDIS_SAK_CACHE = RedisDatabase.TWO;

    private final RedisCacheKlient redisCache;
    private final LRUCache<String, Set<String>> lokalCache;

    // Til testing
    SakCache(RedisCacheKlient redisCache, LRUCache<String, Set<String>> lokalCache) {
        this.redisCache = redisCache;
        this.lokalCache = lokalCache;
    }

    @Inject
    public SakCache() {
        this.redisCache = RedisCacheKlient.instance();
        this.lokalCache = new LRUCache<>(1500, CACHE_DURATION);
    }

    public void store(String key, Set<String> value) {
        var cacheKey = hentCacheKey(key);
        try {
            LOG.debug("Redis storing for key '{}'", cacheKey);
            var joined = String.join(IDENT_INFIX, value).trim();
            redisCache.lagre(cacheKey, joined, CACHE_DURATION, REDIS_SAK_CACHE);
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.put(cacheKey, value);
        }
    }

    public Set<String> read(String key) {
        var cacheKey = hentCacheKey(key);
        try {
            LOG.debug("Redis reading for key: {}", cacheKey);
            var fromCache = redisCache.les(cacheKey, REDIS_SAK_CACHE);

            return fromCache.stream()
                .flatMap(c -> Arrays.stream(c.split(IDENT_INFIX)))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        } catch (RedisCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
        }
        return Optional.ofNullable(lokalCache.get(cacheKey)).orElseGet(Set::of);
    }

    public void remove(String key) {
        var cacheKey = hentCacheKey(key);
        try {
            var verdi = redisCache.les(cacheKey, REDIS_SAK_CACHE);
            if (verdi.isPresent()) {
                LOG.debug("Redis removing key '{}'", cacheKey);
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
        return CACHE_KEY_PREFIX + key;
    }

    private static void logRedisUtilgjengelig() {
        LOG.info("Redis ikke tilgjengelig. Kjører videre med lokal cache.");
    }
}
