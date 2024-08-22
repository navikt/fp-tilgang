package no.nav.foreldrepenger.tilganger.domene.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.tilganger.domene.ansatt.Ansatt;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheUtilgjengeligException;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisDatabase;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.util.LRUCache;

@ExtendWith(MockitoExtension.class)
class AnsattCacheTest {

    @Mock
    private RedisCacheKlient redisKlient;

    @Mock
    private LRUCache<String, Ansatt> lokalCache;

    private AnsattCache cache;

    @BeforeEach
    void setUp() {
        cache = new AnsattCache(redisKlient, lokalCache);
    }

    @Test
    @DisplayName("Lagre i Redis - OK")
    void storeOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var ansatt = lagTestAnsatt(key);
        cache.store(key, ansatt);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.ZERO));
        verify(lokalCache, never()).put(anyString(), eq(ansatt));
    }

    @Test
    @DisplayName("Lagre i Redis - NOK. Lagre i lokal cache - OK.")
    void storeOKWithoutRedisInLocalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).lagre(anyString(), anyString(), anyLong(), any());

        var ansatt = lagTestAnsatt(key);
        cache.store(key, ansatt);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.ZERO));
        verify(lokalCache, times(1)).put(anyString(), eq(ansatt));
    }

    @Test
    @DisplayName("Les fra Redis - OK")
    void readOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var ansatt = lagTestAnsatt(key);

        var cacheKey = AnsattCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE)).thenReturn(Optional.of(DefaultJsonMapper.toJson(ansatt)));
        var cacheResult = cache.read(key);

        assertThat(cacheResult).isPresent();
        assertThat(cacheResult.get()).isEqualTo(ansatt);

        verify(redisKlient, times(1)).les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - NOK. Les fra lokal cache - OK.")
    void readOKWithoutRedisFromLokalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var ansatt = lagTestAnsatt(key);

        var cacheKey = AnsattCache.hentCacheKey(key);
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(ansatt);

        var cacheResult = cache.read(key);

        assertThat(cacheResult).isPresent();
        assertThat(cacheResult.get()).isEqualTo(ansatt);

        verify(redisKlient, times(1)).les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - OK men data er korrupt. Slett key fra Redis - OK. Les fra lokal cache - OK.")
    void readErrorFromRedisMenOkFraLokalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var ansatt = lagTestAnsatt(key);

        var cacheKey = AnsattCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE)).thenReturn(Optional.of("{\"noe\"}"));

        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).fjern(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(ansatt);

        var cacheResult = cache.read(key);

        assertThat(cacheResult).isPresent();
        assertThat(cacheResult.get()).isEqualTo(ansatt);

        verify(redisKlient, times(1)).les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        verify(redisKlient, times(1)).fjern(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - OK men data er korrupt. Slett key fra Redis - NOK. Les fra lokal cache - ingen resultat i cache.")
    void readErrorFromRedisSlettFraCacheIngenTreff() throws RedisCacheUtilgjengeligException {
        var key = "x000000";

        var cacheKey = AnsattCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE)).thenReturn(Optional.of("{\"noe\"}"));

        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).fjern(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(null);

        var cacheResult = cache.read(key);

        assertThat(cacheResult).isEmpty();

        verify(redisKlient, times(1)).les(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        verify(redisKlient, times(1)).fjern(cacheKey, AnsattCache.REDIS_ANSATT_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Evict fra Redis - OK.")
    void flushCache() throws RedisCacheUtilgjengeligException {
        doNothing().when(redisKlient).slettCache(AnsattCache.REDIS_ANSATT_CACHE);
        cache.deleteCache();
        verify(redisKlient, times(1)).slettCache(AnsattCache.REDIS_ANSATT_CACHE);
        verifyNoInteractions(lokalCache);
    }

    @Test
    @DisplayName("Evict fra Redis - NOK.")
    void flushCacheFeil() throws RedisCacheUtilgjengeligException {
        doThrow(new RedisCacheUtilgjengeligException("Exception")).when(redisKlient).slettCache(AnsattCache.REDIS_ANSATT_CACHE);
        cache.deleteCache();
        verify(redisKlient, times(1)).slettCache(AnsattCache.REDIS_ANSATT_CACHE);
        verifyNoInteractions(lokalCache);
    }

    @Test
    @DisplayName("Verifiser at cache key har riktig format.")
    void verifyCacheKey() {
        var key = "x000000";
        var cacheKey = AnsattCache.hentCacheKey(key);
        assertThat(cacheKey).isNotEmpty().isEqualTo(AnsattCache.CACHE_KEY_PREFIX + key);
    }

    private static Ansatt lagTestAnsatt(String key) {
        return new Ansatt(UUID.randomUUID(), key, "Testersen, Test", "Test Testersen", "1234");
    }
}
