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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedigCacheUtilgjengeligException;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisDatabase;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import no.nav.vedtak.util.LRUCache;

import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrupperCacheTest {

    @Mock
    private RedisCacheKlient redisKlient;

    @Mock
    private LRUCache<String, List<UUID>> lokalCache;

    private GrupperCache cache;

    @BeforeEach
    void setUp() {
        cache = new GrupperCache(redisKlient, lokalCache);
    }

    @Test
    @DisplayName("Lagre i Redis - OK")
    void storeOKWithRedis() throws RedigCacheUtilgjengeligException {
        var key = "x000000";
        var grupper = lagAnsattGrupper();
        cache.store(key, grupper);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.ONE));
        verify(lokalCache, never()).put(anyString(), eq(grupper));
    }

    @Test
    @DisplayName("Lagre i Redis - NOK. Lagre i lokal cache - OK.")
    void storeOKWithoutRedisInLocalCache() throws RedigCacheUtilgjengeligException {
        var key = "x000000";
        doThrow(new RedigCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).lagre(anyString(), anyString(), anyLong(), any());

        var grupper = lagAnsattGrupper();
        cache.store(key, grupper);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.ONE));
        verify(lokalCache, times(1)).put(anyString(), eq(grupper));
    }

    @Test
    @DisplayName("Les fra Redis - OK")
    void readOKWithRedis() throws RedigCacheUtilgjengeligException {
        var key = "x000000";
        var grupper = lagAnsattGrupper();

        var cacheKey = GrupperCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE)).thenReturn(Optional.of(DefaultJsonMapper.toJson(grupper)));
        var cacheResult = cache.read(key);

        assertThat(cacheResult).isPresent();
        assertThat(cacheResult.get()).isEqualTo(grupper);

        verify(redisKlient, times(1)).les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - NOK. Les fra lokal cache - OK.")
    void readOKWithoutRedisFromLokalCache() throws RedigCacheUtilgjengeligException {
        var key = "x000000";
        var grupper = lagAnsattGrupper();

        var cacheKey = GrupperCache.hentCacheKey(key);
        doThrow(new RedigCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(grupper);

        var cacheResult = cache.read(key);

        assertThat(cacheResult).isPresent();
        assertThat(cacheResult.get()).isEqualTo(grupper);

        verify(redisKlient, times(1)).les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - OK men data er korrupt. Slett key fra Redis - OK. Les fra lokal cache - OK.")
    void readErrorFromRedisMenOkFraLokalCache() throws RedigCacheUtilgjengeligException {
        var key = "x000000";
        var grupper = lagAnsattGrupper();

        var cacheKey = GrupperCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE)).thenReturn(Optional.of("{\"noe\"}"));

        doThrow(new RedigCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).fjern(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(grupper);

        var cacheResult = cache.read(key);

        assertThat(cacheResult).isPresent();
        assertThat(cacheResult.get()).isEqualTo(grupper);

        verify(redisKlient, times(1)).les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        verify(redisKlient, times(1)).fjern(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - OK men data er korrupt. Slett key fra Redis - NOK. Les fra lokal cache - ingen resultat i cache.")
    void readErrorFromRedisSlettFraCacheIngenTreff() throws RedigCacheUtilgjengeligException {
        var key = "x000000";

        var cacheKey = GrupperCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE)).thenReturn(Optional.of("{\"noe\"}"));

        doThrow(new RedigCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).fjern(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(null);

        var cacheResult = cache.read(key);

        assertThat(cacheResult).isEmpty();

        verify(redisKlient, times(1)).les(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        verify(redisKlient, times(1)).fjern(cacheKey, GrupperCache.REDIS_GRUPPE_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Evict fra Redis - OK.")
    void flushCache() throws RedigCacheUtilgjengeligException {
        doNothing().when(redisKlient).slettCache(GrupperCache.REDIS_GRUPPE_CACHE);
        cache.kasteCache();
        verify(redisKlient, times(1)).slettCache(GrupperCache.REDIS_GRUPPE_CACHE);
        verifyNoInteractions(lokalCache);
    }

    @Test
    @DisplayName("Evict fra Redis - NOK.")
    void flushCacheFeil() throws RedigCacheUtilgjengeligException {
        doThrow(new RedigCacheUtilgjengeligException("Exception")).when(redisKlient).slettCache(GrupperCache.REDIS_GRUPPE_CACHE);
        cache.kasteCache();
        verify(redisKlient, times(1)).slettCache(GrupperCache.REDIS_GRUPPE_CACHE);
        verifyNoInteractions(lokalCache);
    }

    @Test
    @DisplayName("Verifiser at cache key har riktig format.")
    void verifyCacheKey() {
        var key = "x000000";
        var cacheKey = GrupperCache.hentCacheKey(key);
        assertThat(cacheKey).isNotEmpty().isEqualTo(GrupperCache.CACHE_KEY_PREFIX + key);
    }

    private static List<UUID> lagAnsattGrupper() {
        return List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}
