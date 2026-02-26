package com.middleware.redis.component;

import com.middleware.redis.exception.RedisOpertionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RedisOperationManager {

    @Autowired
    private RedisConnectionManager redisConnectionManager;

    public void write(RedisCommands redis, String redisKey, Map<String, String> attributes)
            throws RedisOpertionException {

        boolean write = false;
        int tries = 0;
        while (!write && tries < 3) {
            try {
                redis.hmset(redisKey, attributes);
                write = true;
            } catch (Exception ex) {
                if (tries < 2) {
                    redis = redisConnectionManager.reconnect(redis);
                } else {
                    log.error("Can't wrtie " + redisKey, ex);
                    throw new RedisOpertionException("Can't wrtie " + redisKey, ex);
                }
            } finally {
                tries++;
            }
        }
    }

    public void writeValue(RedisCommands redis, String redisKey, String jsonValue) {

        boolean write = false;
        int tries = 0;
        while (!write && tries < 3) {
            try {
                redis.set(redisKey, jsonValue);
                write = true;
            } catch (Exception ex) {
                if (tries < 2) {
                    redis = redisConnectionManager.reconnect(redis);
                } else {
                    log.error("Can't wrtie " + redisKey, ex);
                    throw new RedisOpertionException("Can't wrtie " + redisKey, ex);
                }
            } finally {
                tries++;
            }
        }
    }

    public void writeValues(RedisCommands redis, List<Pair<String, String>> redisKeyJsonPairs) {

        boolean write = false;
        int tries = 0;
        while (!write && tries < 3) {
            try {
                redis.setAll(redisKeyJsonPairs.toArray(new ImmutablePair[redisKeyJsonPairs.size()]));
                write = true;
            } catch (Exception ex) {
                if (tries < 2) {
                    redis = redisConnectionManager.reconnect(redis);
                } else {
                    log.error("Can't wrtie multiple " + redisKeyJsonPairs, ex);
                    throw new RedisOpertionException("Can't wrtie multiple " + redisKeyJsonPairs, ex);
                }
            } finally {
                tries++;
            }
        }
    }

    public Map<String, String> read(RedisCommands redis, String redisKey) {

        boolean read = false;
        int tries = 0;
        Map<String, String> attributes = null;
        while (!read && tries < 3) {
            try {
                attributes = redis.hgetAll(redisKey);
                read = true;
            } catch (Exception ex) {
                if (tries < 2) {
                    redis = redisConnectionManager.reconnect(redis);
                } else {
                    log.error("Can't read " + redisKey, ex);
                    throw new RedisOpertionException("Can't read " + redisKey, ex);
                }
            } finally {
                tries++;
            }
        }
        return attributes;
    }

    public String readValue(RedisCommands redis, String redisKey) {

        boolean read = false;
        int tries = 0;
        String value = null;
        while (!read && tries < 3) {
            try {
                value = redis.get(redisKey);
                read = true;
            } catch (Exception ex) {
                if (tries < 2) {
                    redis = redisConnectionManager.reconnect(redis);
                } else {
                    log.error("Can't read " + redisKey, ex);
                    throw new RedisOpertionException("Can't read " + redisKey, ex);
                }
            } finally {
                tries++;
            }
        }
        return value;
    }

    public List<String> readValues(RedisCommands redis, List<String> redisKeys) {

        boolean read = false;
        int tries = 0;
        List<String> value = null;
        while (!read && tries < 3) {
            try {
                value = redis.getAll(redisKeys.toArray(new String[redisKeys.size()]));
                read = true;
            } catch (Exception ex) {
                if (tries < 2) {
                    redis = redisConnectionManager.reconnect(redis);
                } else {
                    log.error("Can't read multiple keys " + redisKeys, ex);
                    throw new RedisOpertionException("Can't read " + redisKeys, ex);
                }
            } finally {
                tries++;
            }
        }
        return value;
    }

    public void deleteAndWrite(RedisCommands redisCommands, String key, Map<String, String> attributes)
            throws RedisOpertionException {

        delete(redisCommands, key);
        if (!attributes.isEmpty()) {
            write(redisCommands, key, attributes);
        }
    }

    public void delete(RedisCommands redis, String... key) throws RedisOpertionException {

        boolean delete = false;
        int tries = 0;
        while (!delete && tries < 3) {
            try {
                redis.del(key);
                delete = true;
            } catch (Exception ex) {
                if (tries < 2) {
                    redis = redisConnectionManager.reconnect(redis);
                } else {
                    throw new RedisOpertionException("Error deleting key " + key);
                }
            } finally {
                tries++;
            }
        }
    }

}