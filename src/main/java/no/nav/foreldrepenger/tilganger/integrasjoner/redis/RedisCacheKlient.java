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

    private static RedisCacheKlient INSTANCE;

    private final JedisPool jedisPool;

    private RedisCacheKlient(String host, int port, String pass) {
        var jedisConfig = DefaultJedisClientConfig.builder().password(pass).build();
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofSeconds(3));
        poolConfig.setTestOnBorrow(true);
        LOG.info("Creating JedisPool");
        jedisPool = new JedisPool(poolConfig, new HostAndPort(host, port), jedisConfig);
        LOG.debug("JEDIS pool active: {}", !jedisPool.isClosed());
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

    public void lagre(String key, String value, long expiresInSeconds, RedisDatabase redisDatabase) throws RedisCacheUtilgjengeligException {
        Objects.requireNonNull(key, "store cache key is null");
        Objects.requireNonNull(value, "store cache value is null");
        Objects.requireNonNull(redisDatabase, "store database value is null");
        try (var jedis = getJedisPool().getResource()) {
            jedis.select(mapToDatabaseNumber(redisDatabase));
            LOG.debug("Storing key {} with value {}", key, value);
            jedis.set(key, value, SetParams.setParams().ex(expiresInSeconds));
        } catch (Exception e) {
            throwRedisUtilgjengeligException(e);
        }
    }

    public Optional<String> les(String key, RedisDatabase redisDatabase) throws RedisCacheUtilgjengeligException {
        Objects.requireNonNull(key, "read cache key is null");
        try (var jedis = getJedisPool().getResource()) {
            jedis.select(mapToDatabaseNumber(redisDatabase));
            if (jedis.exists(key)) {
                LOG.debug("Reading key {} from pool", key);
                return Optional.of(jedis.get(key));
            }
            LOG.debug("Finner ikke key {}", key);
        } catch (Exception e) {
            throwRedisUtilgjengeligException(e);
        }
        return Optional.empty();
    }

    public void fjern(String key, RedisDatabase redisDatabase) throws RedisCacheUtilgjengeligException {
        Objects.requireNonNull(key, "remove cache key is null");
        try (var jedis = getJedisPool().getResource()) {
            jedis.select(mapToDatabaseNumber(redisDatabase));
            LOG.debug("Fjerne key {}", key);
            jedis.del(key);
        } catch (Exception e) {
            throwRedisUtilgjengeligException(e);
        }
    }

    public void slettCache(RedisDatabase redisDatabase) throws RedisCacheUtilgjengeligException {
        try (var jedis = getJedisPool().getResource()) {
            LOG.debug("Fjerner hele cachen i database {}", redisDatabase);
            jedis.select(mapToDatabaseNumber(redisDatabase));
            jedis.flushDB(FlushMode.ASYNC);
        } catch (Exception e) {
            throwRedisUtilgjengeligException(e);
        }
    }

    private JedisPool getJedisPool() {
        return jedisPool;
    }

    private int mapToDatabaseNumber(RedisDatabase redisDatabase) {
        return switch (redisDatabase) {
            case ZERO -> 0;
            case ONE -> 1;
            case TWO -> 2;
            case THREE -> 3;
            case FOUR -> 4;
            case FIVE -> 5;
            case SIX -> 6;
            case SEVEN -> 7;
            case EIGHT -> 8;
            case NINE -> 9;
            case TEN -> 10;
            case ELEVEN -> 11;
            case TWELVE -> 12;
            case THIRTEEN -> 13;
            case FOURTEEN -> 14;
            case FIFTEEN -> 15;
        };
    }

    private static void throwRedisUtilgjengeligException(Exception e) throws RedisCacheUtilgjengeligException {
        throw new RedisCacheUtilgjengeligException("Redis utilgjengelig.", e);
    }
}
