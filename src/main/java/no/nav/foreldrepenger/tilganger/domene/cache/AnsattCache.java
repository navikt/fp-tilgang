package no.nav.foreldrepenger.tilganger.domene.cache;

import java.time.Duration;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedigCacheUtilgjengeligException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.tilganger.domene.ansatt.Ansatt;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisDatabase;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class AnsattCache {

    private static final Logger LOG = LoggerFactory.getLogger(AnsattCache.class);
    private static final long CACHE_DURATION = Duration.ofDays(30).getSeconds();
    static final String CACHE_KEY_PREFIX = "ansatt_";
    static final RedisDatabase REDIS_ANSATT_CACHE = RedisDatabase.ZERO;

    private final RedisCacheKlient redisCache;
    private final LRUCache<String, Ansatt> lokalCache;

    AnsattCache(RedisCacheKlient redisCache, LRUCache<String, Ansatt> lokalCache) {
        this.redisCache = redisCache;
        this.lokalCache = lokalCache;
    }

    @Inject
    public AnsattCache() {
        this.redisCache = RedisCacheKlient.instance();
        this.lokalCache = new LRUCache<>(1500, CACHE_DURATION);
    }

    public void store(String key, Ansatt value) {
        var cacheKey = hentCacheKey(key);
        try {
            redisCache.lagre(cacheKey, DefaultJsonMapper.toJson(value), CACHE_DURATION, REDIS_ANSATT_CACHE);
        } catch (RedigCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
            lokalCache.put(cacheKey, value);
        }
    }

    public Optional<Ansatt> read(String key) {
        var cacheKey = hentCacheKey(key);
        try {
            return redisCache.les(cacheKey, REDIS_ANSATT_CACHE).map(value -> DefaultJsonMapper.fromJson(value, Ansatt.class));
        } catch (TekniskException tex) {
            LOG.info("Feil ved deserialisering av ansatt. Fjerner key fra cache.");
            try {
                redisCache.fjern(cacheKey, REDIS_ANSATT_CACHE);
            } catch (RedigCacheUtilgjengeligException e) {
                logRedisUtilgjengelig();
            }
        } catch (RedigCacheUtilgjengeligException e) {
            logRedisUtilgjengelig();
        }
        return Optional.ofNullable(lokalCache.get(cacheKey));
    }

    public void deleteCache() {
        LOG.info("Fjerner ansatt cache.");
        try {
            redisCache.slettCache(REDIS_ANSATT_CACHE);
        } catch (RedigCacheUtilgjengeligException e) {
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
