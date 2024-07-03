package no.nav.foreldrepenger.tilganger.integrasjoner.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.params.SetParams;

public class RedisCacheKlient {
    private static final Logger LOG = LoggerFactory.getLogger(RedisCacheKlient.class);
    private static final Environment ENV = Environment.current();
    private static final int DEFAULT_CACHE_DURATION_IN_MINUTES = 60;

    private static RedisCacheKlient INSTANCE;

    private JedisPool jedisPool;

    private RedisCacheKlient(String host, int port, String pass) {
        var jedisConfig = DefaultJedisClientConfig.builder()
            .password(pass)
            .build();
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofSeconds(3));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        LOG.debug("Creating JedisPool with config.");
        jedisPool = new JedisPool(poolConfig, new HostAndPort(host, port), jedisConfig);
        LOG.debug("JEDIS pool closed: {}", jedisPool.isClosed());
    }

    public static synchronized RedisCacheKlient instance() {
        var inst = INSTANCE;
        if (inst == null) {
            var host = ENV.getProperty("redis.host");
            var port = ENV.getProperty("redis.port", Integer.class);
            var pass = ENV.getProperty("redis.password");
            inst = new RedisCacheKlient(host, port, pass);
            INSTANCE = inst;
        }
        return inst;
    }

    /**
     * Lagres i 60 minutter
     */
    public void store(String key, String value, int database) {
        Objects.requireNonNull(key, "store cache key is null");
        Objects.requireNonNull(value, "store cache value is null");
        store(key, value, Duration.ofMinutes(DEFAULT_CACHE_DURATION_IN_MINUTES).getSeconds(), database);
    }

    public void store(String key, String value, long expiresInSeconds, int database) {
        Objects.requireNonNull(key, "store cache key is null");
        Objects.requireNonNull(value, "store cache value is null");
        try (var jedis = getJedisPool().getResource()) {
            jedis.select(database);
            LOG.debug("Storing key {} with value {}", key, value);
            jedis.set(key, value, SetParams.setParams().ex(expiresInSeconds));
        } catch (Exception e) {
            LOG.info("Feil ved lagring i redis: {}. Kjører videre uten cache.", e.getMessage());
            throw e;
        }
    }

    public Optional<String> read(String key, int database) {
        Objects.requireNonNull(key, "read cache key is null");
        try (var jedis = getJedisPool().getResource()) {
            jedis.select(database);
            if (jedis.exists(key)) {
                LOG.debug("Reading key {} from pool", key);
                return Optional.of(jedis.get(key));
            }
            LOG.debug("Finner ikke key {}", key);
        } catch (Exception e) {
            LOG.info("Feil ved lesing fra redis: {}. Kjører videre uten cache.", e.getMessage());
            throw e;
        }
        return Optional.empty();
    }

    public void remove(String key, int database) {
        Objects.requireNonNull(key, "remove cache key is null");
        try (var jedis = getJedisPool().getResource()) {
            jedis.select(database);
            LOG.debug("Fjerne key {}", key);
            jedis.del(key);
        } catch (Exception e) {
            LOG.info("Feil ved sletting fra redis: {}. Kjører videre uten cache.", e.getMessage());
            throw e;
        }
    }

    public void evictCacheIn(int database) {
        try (var jedis = getJedisPool().getResource()) {
            LOG.debug("Fjerner hele cachen i database {}", database);
            jedis.select(database);
            jedis.flushDB(FlushMode.ASYNC);
        } catch (Exception e) {
            LOG.info("Feil ved flushing av redis: {}. Kjører videre uten cache.", e.getMessage());
            throw e;
        }
    }

    private JedisPool getJedisPool() {
        return jedisPool;
    }
}
