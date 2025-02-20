package com.core.redis.component;

import org.apache.commons.lang3.tuple.Pair;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RedisCommands {

    private Jedis jedis;
    private JedisCluster jedisCluster;

    public RedisCommands(Jedis jedis) {

        this.jedis = jedis;
    }

    public RedisCommands(JedisCluster jedisCluster) {

        this.jedisCluster = jedisCluster;
    }

    public String set(String s, String s1) {

        if (jedis != null) {
            return jedis.set(s, s1);
        } else {
            return jedisCluster.set(s, s1);
        }
    }

    public String setAll(Pair<String, String>... keyValuesPair) {

        Collection<String> keyValues = new ArrayList<>();
        Arrays.stream(keyValuesPair).forEach(kv -> {
            keyValues.add(kv.getKey());
            keyValues.add(kv.getValue());
        });
        if (jedis != null) {
            return jedis.mset(keyValues.toArray(new String[keyValues.size()]));
        } else {
            return jedisCluster.mset(keyValues.toArray(new String[keyValues.size()]));
        }
    }

    public String set(String s, String s1, SetParams setParams) {

        if (jedis != null) {
            return jedis.set(s, s1, setParams);
        } else {
            return jedisCluster.set(s, s1, setParams);
        }
    }

    public String get(String s) {

        if (jedis != null) {
            return jedis.get(s);
        } else {
            return jedisCluster.get(s);
        }
    }

    public List<String> getAll(String... s) {

        if (jedis != null) {
            return jedis.mget(s);
        } else {
            return jedisCluster.mget(s);
        }
    }

    public String hmset(String s, Map<String, String> map) {

        if (jedis != null) {
            return jedis.hmset(s, map);
        } else {
            return jedisCluster.hmset(s, map);
        }

    }

    public Map<String, String> hgetAll(String s) {

        if (jedis != null) {
            return jedis.hgetAll(s);
        } else {
            return jedisCluster.hgetAll(s);
        }

    }

    public Long del(String... s) {

        if (jedis != null) {
            return jedis.del(s);
        } else {
            return jedisCluster.del(s);
        }
    }


    public String echo(String s) {

        if (jedis != null) {
            return jedis.echo(s);
        } else {
            return jedisCluster.echo(s);
        }
    }

    public Object eval(String script, List<String> keys, List<String> args) {

        if (jedis != null) {
            return jedis.eval(script, keys, args);
        } else {
            return jedisCluster.eval(script, keys, args);
        }
    }

    public JedisCommands getJedis() {

        return jedis;
    }

    public JedisCluster getJedisCluster() {

        return jedisCluster;
    }
}
