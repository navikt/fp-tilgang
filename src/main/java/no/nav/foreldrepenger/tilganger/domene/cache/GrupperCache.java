package no.nav.foreldrepenger.tilganger.domene.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class GrupperCache {

    private static final Logger LOG = LoggerFactory.getLogger(GrupperCache.class);
    private static final long CACHE_DURATION = Duration.ofHours(1).getSeconds();
    static final String CACHE_KEY_PREFIX = "grupper_";
    static final int DB_NUMBER = 1;

    private final RedisCacheKlient redisCache;
    private final LRUCache<String, Set<UUID>> lokalCache;

    @Inject
    public GrupperCache() {
        this.redisCache = RedisCacheKlient.instance();
        this.lokalCache = new LRUCache<>(1500, CACHE_DURATION);
    }

    public void store(String key, Set<UUID> value) {
        var cacheKey = hentCacheKey(key);
        try {
            LOG.debug("Redis storing for key '{}'", cacheKey);
            redisCache.store(cacheKey, DefaultJsonMapper.toJson(value), DB_NUMBER);
        } catch (Exception e) {
            logRedisUtilgjengelig();
            lokalCache.put(cacheKey, value);
        }
    }

    public Optional<Set<UUID>> read(String key) {
        var cacheKey = hentCacheKey(key);
        try {
            LOG.debug("Redis reading for key '{}'", cacheKey);
            return redisCache.read(cacheKey, DB_NUMBER).map(value -> DefaultJsonMapper.fromJson(value, Set.class));
        } catch (TekniskException tex) {
            LOG.info("Feil ved deserialisering av grupper. Fjerner key fra cache.");
            redisCache.remove(cacheKey, DB_NUMBER);
            throw tex;
        } catch (Exception e) {
            logRedisUtilgjengelig();
            return Optional.ofNullable(lokalCache.get(cacheKey));
        }
    }

    public void flushAnsattCache() {
        LOG.info("Flushing ansatt cache");
        try {
            redisCache.evictCacheIn(DB_NUMBER);
        } catch (Exception e) {
            logRedisUtilgjengelig();
        }
    }

    private static String hentCacheKey(String key) {
        return CACHE_KEY_PREFIX + key;
    }

    private static void logRedisUtilgjengelig() {
        LOG.info("Redis ikke tilgjengelig. Kjører videre med lokalt cache.");
    }
}
