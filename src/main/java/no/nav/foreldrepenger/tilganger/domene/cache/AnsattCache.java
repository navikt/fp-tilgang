package no.nav.foreldrepenger.tilganger.domene.cache;

import java.time.Duration;
import java.util.Optional;

import no.nav.foreldrepenger.tilganger.domene.Ansatt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
@CacheAnsatt
public class AnsattCache implements Cache<Ansatt> {

    private static final Logger LOG = LoggerFactory.getLogger(AnsattCache.class);
    private static final long CACHE_DURATION = Duration.ofDays(30).getSeconds();
    static final String CACHE_KEY_PREFIX = "ansatt_";

    private final RedisCacheKlient redisCache;
    private final LRUCache<String, Ansatt> lokalCache;

    @Inject
    public AnsattCache() {
        this.redisCache = RedisCacheKlient.instance();
        this.lokalCache = new LRUCache<>(1500, CACHE_DURATION);
    }

    @Override
    public void store(String key, Ansatt value) {
        var cacheKey = hentCacheKey(key);
        try {
            redisCache.store(cacheKey, DefaultJsonMapper.toJson(value), CACHE_DURATION);
        } catch (Exception e) {
            logRedisUtilgjengelig();
            lokalCache.put(cacheKey, value);
        }
    }

    @Override
    public Optional<Ansatt> read(String key) {
        var cacheKey = hentCacheKey(key);
        try {
            return redisCache.read(cacheKey).map(value -> DefaultJsonMapper.fromJson(value, Ansatt.class));
        } catch (Exception e) {
            logRedisUtilgjengelig();
            return Optional.ofNullable(lokalCache.get(cacheKey));
        }
    }

    private static String hentCacheKey(String key) {
        return CACHE_KEY_PREFIX + key;
    }

    private static void logRedisUtilgjengelig() {
        LOG.info("Redis ikke tilgjengelig. Kjører videre med lokalt cache.");
    }
}
