package no.nav.foreldrepenger.tilganger.domene.cache;

import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheUtilgjengeligException;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisDatabase;
import no.nav.vedtak.util.LRUCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SakCacheTest {

    private static final String[] IDENTER = { "3234567890123", "4234567890124", "5234567890125", "6234567890126" };

    @Mock
    private RedisCacheKlient redisKlient;

    @Mock
    private LRUCache<String, Set<String>> lokalCache;

    private SakCache cache;

    @BeforeEach
    void setUp() {
        cache = new SakCache(redisKlient, lokalCache);
    }

    @Test
    @DisplayName("Lagre i Redis - OK")
    void storeOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(3);
        cache.store(key, identer);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, never()).put(anyString(), eq(identer));
    }

    @Test
    @DisplayName("Lagre i Redis - NOK. Lagre i lokal cache - OK.")
    void storeOKWithoutRedisInLocalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).lagre(anyString(), anyString(), anyLong(), any());

        var identer = lagIdenter(3);
        cache.store(key, identer);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, times(1)).put(anyString(), eq(identer));
    }

    @Test
    @DisplayName("Les fra Redis - OK")
    void readOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(4);

        var cacheKey = SakCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(String.join(":", identer)));
        var cacheResult = cache.read(key);

        assertThat(cacheResult).hasSize(4)
            .containsAll(identer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 1 ident - OK")
    void readOKWithRedisOneIdent() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(1);

        var cacheKey = SakCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(String.join(":", identer)));
        var cacheResult = cache.read(key);

        assertThat(cacheResult).hasSize(1)
            .containsAll(identer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 0 ident - OK")
    void readOKWithRedisZeroIdent() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(0);

        var cacheKey = SakCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(String.join(":", identer)));
        var cacheResult = cache.read(key);

        assertThat(cacheResult).isEmpty();

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - NOK. Les fra lokal cache - OK.")
    void readOKWithoutRedisFromLokalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(3);

        var cacheKey = SakCache.hentCacheKey(key);
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(identer);

        var cacheResult = cache.read(key);

        assertThat(cacheResult).hasSize(3)
            .containsAll(identer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Evict fra Redis - OK.")
    void flushCache() throws RedisCacheUtilgjengeligException {
        doNothing().when(redisKlient).slettCache(SakCache.REDIS_SAK_CACHE);
        cache.deleteCache();
        verify(redisKlient, times(1)).slettCache(SakCache.REDIS_SAK_CACHE);
        verifyNoInteractions(lokalCache);
    }

    @Test
    @DisplayName("Evict fra Redis - NOK.")
    void flushCacheFeil() throws RedisCacheUtilgjengeligException {
        doThrow(new RedisCacheUtilgjengeligException("Exception")).when(redisKlient).slettCache(SakCache.REDIS_SAK_CACHE);
        cache.deleteCache();
        verify(redisKlient, times(1)).slettCache(SakCache.REDIS_SAK_CACHE);
        verifyNoInteractions(lokalCache);
    }

    @Test
    @DisplayName("Verifiser at cache key har riktig format.")
    void verifyCacheKey() {
        var key = "123456789";
        var cacheKey = SakCache.hentCacheKey(key);
        assertThat(cacheKey).isNotEmpty().isEqualTo(SakCache.CACHE_KEY_PREFIX + key);
    }

    private static Set<String> lagIdenter(int antall) {
        return new HashSet<>(Arrays.asList(IDENTER).subList(0, antall));
    }
}
