package no.nav.foreldrepenger.tilganger.integrasjoner.cache;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.konfig.Environment;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

@ApplicationScoped
public class CacheTjeneste implements Cache {
    private static final Logger LOG = LoggerFactory.getLogger(CacheTjeneste.class);

    private static final Environment ENV = Environment.current();

    private JedisPool jedisPool;

    public CacheTjeneste() {
        var host = ENV.getProperty("REDIS_HOST","localhost");
        var port = ENV.getProperty("REDIS_PORT", Integer.class, 6379);
        var config = DefaultJedisClientConfig.builder()
            .password(ENV.getProperty("REDIS_PASSWORD"))
            .ssl(!ENV.isLocal())
            .hostnameVerifier((hostname, session) -> {
                var evaluering = hostname.equals(host);
                LOG.info("Evaluating hostname {} for {}", hostname, evaluering);
                return evaluering;
            }).build();
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofSeconds(3));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        jedisPool = new JedisPool(poolConfig, new HostAndPort(host, port), config);
    }

    @Override
    public void store(String key, String value) {
        try (var jedis = jedisPool.getResource()) {
            LOG.debug("Storing key {} with value {}", key, value);
            jedis.set(key, value, SetParams.setParams().ex(Duration.ofMinutes(60).getSeconds()));
        }
    }

    @Override
    public Optional<String> read(String key) {
        try (var jedis = jedisPool.getResource()) {
            if (jedis.exists(key)) {
                LOG.debug("Reading key {} from pool", key);
                return Optional.of(jedis.get(key));
            }
            LOG.debug("Finner ikke key {}", key);
            return Optional.empty();
        }
    }

}
