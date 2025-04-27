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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheKlient;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisCacheUtilgjengeligException;
import no.nav.foreldrepenger.tilganger.integrasjoner.redis.RedisDatabase;
import no.nav.vedtak.util.LRUCache;

@ExtendWith(MockitoExtension.class)
class SakCacheTest {

    private static final String[] IDENTER = { "3234567890123", "4234567890124", "5234567890125", "6234567890126" };

    @Mock
    private RedisCacheKlient redisKlient;

    @Mock
    private LRUCache<String, String> lokalCache;

    private SakCache cache;

    @BeforeEach
    void setUp() {
        cache = new SakCache(redisKlient, lokalCache);
    }

    @Test
    @DisplayName("Lagre i Redis - OK")
    void storeIdenterOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(3);
        cache.storeIdenter(key, identer);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, never()).put(anyString(), eq(String.join(":", identer)));
    }

    @Test
    @DisplayName("Lagre i Redis - NOK. Lagre i lokal cache - OK.")
    void storeIdenterOKWithoutRedisInLocalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).lagre(anyString(), anyString(), anyLong(), any());

        var identer = lagIdenter(3);
        cache.storeIdenter(key, identer);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, times(1)).put(anyString(), eq(String.join(":", identer)));
    }

    @Test
    @DisplayName("Les fra Redis - OK")
    void readIdenterOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(4);

        var cacheKey = SakCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(String.join(":", identer)));
        var cacheResult = cache.readIdenter(key);

        assertThat(cacheResult).hasSize(4)
            .containsAll(identer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 1 ident - OK")
    void readIdenterOKWithRedisOneIdent() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(1);

        var cacheKey = SakCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(String.join(":", identer)));
        var cacheResult = cache.readIdenter(key);

        assertThat(cacheResult).hasSize(1)
            .containsAll(identer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 0 ident - OK")
    void readIdenterOKWithRedisZeroIdent() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(0);

        var cacheKey = SakCache.hentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(String.join(":", identer)));
        var cacheResult = cache.readIdenter(key);

        assertThat(cacheResult).isEmpty();

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - NOK. Les fra lokal cache - OK.")
    void readIdenterOKWithoutRedisFromLokalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var identer = lagIdenter(3);

        var cacheKey = SakCache.hentCacheKey(key);
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(String.join(":", identer));

        var cacheResult = cache.readIdenter(key);

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
        assertThat(cacheKey).isNotEmpty().isEqualTo(SakCache.CACHE_KEY_IDENTER_PREFIX + key);
    }

    private static Set<String> lagIdenter(int antall) {
        return new HashSet<>(Arrays.asList(IDENTER).subList(0, antall));
    }

    private static String lagSaksident(int pos) {
        return IDENTER[pos];
    }

    @Test
    @DisplayName("Lagre i Redis - OK")
    void storeSakidentOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var saksident = lagSaksident(1);
        cache.storeSaksident(key, saksident);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Lagre i Redis - NOK. Lagre i lokal cache - OK.")
    void storeSakidentOKWithoutRedisInLocalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).lagre(anyString(), anyString(), anyLong(), any());

        var saksident = lagSaksident(3);
        cache.storeSaksident(key, saksident);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, times(1)).put(anyString(), eq(saksident));
    }

    @Test
    @DisplayName("Les fra Redis - OK")
    void readSakidentOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var saksident = lagSaksident(0);

        var cacheKey = SakCache.hentSaksidentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(saksident));
        var cacheResult = cache.readSaksident(key);

        assertThat(cacheResult).isEqualTo(saksident);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 1 ident - OK")
    void readSakidentOKWithRedisOneIdent() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var saksident = lagSaksident(1);

        var cacheKey = SakCache.hentSaksidentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(saksident));
        var cacheResult = cache.readSaksident(key);

        assertThat(cacheResult).isEqualTo(saksident);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 0 ident - OK")
    void readSakidentOKWithRedisZeroIdent() throws RedisCacheUtilgjengeligException {
        var key = "x000000";

        var cacheKey = SakCache.hentSaksidentCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.empty());
        var cacheResult = cache.readSaksident(key);

        assertThat(cacheResult).isNull();

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - NOK. Les fra lokal cache - OK.")
    void readSakidentOKWithoutRedisFromLokalCache() throws RedisCacheUtilgjengeligException {
        var key = "x000000";
        var saksident = lagSaksident(3);

        var cacheKey = SakCache.hentSaksidentCacheKey(key);
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(saksident);

        var cacheResult = cache.readSaksident(key);

        assertThat(cacheResult).isEqualTo(saksident);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Verifiser at cache key har riktig format.")
    void verifySakidentCacheKey() {
        var key = "123456789";
        var cacheKey = SakCache.hentSaksidentCacheKey(key);
        assertThat(cacheKey).isNotEmpty().isEqualTo(SakCache.CACHE_KEY_SAKSIDENT_PREFIX + key);
    }


    @Test
    @DisplayName("Lagre i Redis - OK")
    void storeBehandlingOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = UUID.randomUUID();
        var saksnummer = "123456789";
        cache.storeBehandlingSaksnummer(key, saksnummer);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Lagre i Redis - NOK. Lagre i lokal cache - OK.")
    void storeBehandlingOKWithoutRedisInLocalCache() throws RedisCacheUtilgjengeligException {
        var key = UUID.randomUUID();
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).lagre(anyString(), anyString(), anyLong(), any());

        var saksnummer = "123456789";
        cache.storeBehandlingSaksnummer(key, saksnummer);
        verify(redisKlient, times(1)).lagre(anyString(), anyString(), anyLong(), eq(RedisDatabase.TWO));
        verify(lokalCache, times(1)).put(anyString(), eq(saksnummer));
    }

    @Test
    @DisplayName("Les fra Redis - OK")
    void readBehandlingOKWithRedis() throws RedisCacheUtilgjengeligException {
        var key = UUID.randomUUID();
        var saksnummer = "123456789";

        var cacheKey = SakCache.hentBehandlingCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(saksnummer));
        var cacheResult = cache.readBehandlingSaksnummer(key);

        assertThat(cacheResult).isEqualTo(saksnummer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 1 ident - OK")
    void readBehandlingOKWithRedisOneIdent() throws RedisCacheUtilgjengeligException {
        var key = UUID.randomUUID();
        var saksnummer = "123456789";

        var cacheKey = SakCache.hentBehandlingCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.of(saksnummer));
        var cacheResult = cache.readBehandlingSaksnummer(key);

        assertThat(cacheResult).isEqualTo(saksnummer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis 0 ident - OK")
    void readBehandlingOKWithRedisZeroIdent() throws RedisCacheUtilgjengeligException {
        var key = UUID.randomUUID();

        var cacheKey = SakCache.hentBehandlingCacheKey(key);
        when(redisKlient.les(cacheKey, SakCache.REDIS_SAK_CACHE)).thenReturn(Optional.empty());
        var cacheResult = cache.readBehandlingSaksnummer(key);

        assertThat(cacheResult).isNull();

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, never()).get(cacheKey);
    }

    @Test
    @DisplayName("Les fra Redis - NOK. Les fra lokal cache - OK.")
    void readBehandlingOKWithoutRedisFromLokalCache() throws RedisCacheUtilgjengeligException {
        var key = UUID.randomUUID();
        var saksnummer = "123456789";

        var cacheKey = SakCache.hentBehandlingCacheKey(key);
        doThrow(new RedisCacheUtilgjengeligException("Redis utilgjengelig")).when(redisKlient).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        when(lokalCache.get(cacheKey)).thenReturn(saksnummer);

        var cacheResult = cache.readBehandlingSaksnummer(key);

        assertThat(cacheResult).isEqualTo(saksnummer);

        verify(redisKlient, times(1)).les(cacheKey, SakCache.REDIS_SAK_CACHE);
        verify(lokalCache, times(1)).get(cacheKey);
    }

    @Test
    @DisplayName("Verifiser at cache key har riktig format.")
    void verifyBehandlingCacheKey() {
        var key = UUID.randomUUID();
        var cacheKey = SakCache.hentBehandlingCacheKey(key);
        assertThat(cacheKey).isNotEmpty().isEqualTo(SakCache.CACHE_KEY_BEHANDLING_PREFIX + key);
    }

}
