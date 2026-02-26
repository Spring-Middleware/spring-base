package com.middleware.redis.component;

import com.middleware.redis.config.RedisConnectionParameters;
import com.middleware.redis.exception.JedisConnectionException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

@Slf4j
@Component
public class RedisConnectionManager {

    @Autowired
    private RedisConnectionParameters redisConnectionParameters;
    private RedisCommands redis;
    private JedisCluster jedisCluster;
    private JedisPool jedisPool = null;

    @PostConstruct
    private void startJedis() {

        if (redisConnectionParameters.isCluster()) {
            jedisCluster = new JedisCluster(
                    new HostAndPort(redisConnectionParameters.getHost(), redisConnectionParameters.getPort()),
                    redisConnectionParameters.getTimeout(),
                    redisConnectionParameters.getGenericObjectPoolConfig());

        } else {
            JedisPool newJedisPool = createJedisPool();
            if (jedisPool != null) {
                jedisPool.close();
            }
            jedisPool = newJedisPool;
        }
    }

    private RedisCommands createRedis(RedisCommands redis) {

        if (redisConnectionParameters.isCluster()) {
            return new RedisCommands(new JedisCluster(
                    new HostAndPort(redisConnectionParameters.getHost(), redisConnectionParameters.getPort()),
                    redisConnectionParameters.getTimeout(),
                    redisConnectionParameters.getGenericObjectPoolConfig()));

        } else {
            if (redis != null) {
                ((Jedis) redis.getJedis()).close();
            }
            return new RedisCommands(jedisPool.getResource());
        }
    }

    private JedisPool createJedisPool() {

        return new JedisPool(
                redisConnectionParameters.getGenericObjectPoolConfig(),
                redisConnectionParameters.getHost(),
                redisConnectionParameters.getPort(),
                redisConnectionParameters.getTimeout(),
                null, // Must be NULL to avoid credentials with empty password
                redisConnectionParameters.getDatabase());
    }

    public RedisCommands getConnection() {

        if (redisConnectionParameters.isCluster()) {
            return new RedisCommands(jedisCluster);
        } else {
            return new RedisCommands(jedisPool.getResource());
        }
    }

    public void closeConnection(RedisCommands redisCommands) {

        try {
            if (redisConnectionParameters.isCluster()) {
                redisCommands.getJedisCluster().close();
            } else {
                ((Jedis) redisCommands.getJedis()).close();
            }
        } catch (Throwable ex) {
            log.error("Error closing connection with redis", ex);
        }
    }

    public void testConnection() throws JedisConnectionException {

        RedisCommands redisCommands = null;
        try {
            redisCommands = getConnection();
            if (!redisCommands.echo("OK").equals("OK")) {
                throw new JedisConnectionException("Connection with redis FAILED");
            } else {
                log.info("Connected with REDIS");
            }
        } catch (Exception ex) {
            throw new JedisConnectionException("Connection with redis FAILED", ex);
        } finally {
            if (redisCommands != null) {
                closeConnection(redisCommands);
            }
        }
    }

    public RedisCommands reconnect(RedisCommands redisCommands) throws JedisConnectionException {

        boolean connect = false;
        long time = System.currentTimeMillis();
        Exception exception = null;
        while (!connect && (System.currentTimeMillis() - time) < redisConnectionParameters.getMaxReconnectiongTime()) {
            try {
                redis = createRedis(redisCommands);
                connect = redis.echo("OK").equals("OK");
                log.info(" Reconnected with Redis: " + connect);
            } catch (Exception e) {
                exception = e;
                try {
                    Thread.sleep(redisConnectionParameters.getWaitTimeBetweenRetries());
                } catch (InterruptedException iex) {
                    log.error("Erros slepping thread", iex);
                }
            }
        }
        if (!connect) {
            throw new JedisConnectionException(
                    ("Can't reconect with redis: " + exception != null ? exception.getMessage() : ""));
        }
        return redis;
    }

}
