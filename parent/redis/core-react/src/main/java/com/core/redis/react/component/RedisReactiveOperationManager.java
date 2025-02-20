package com.core.redis.react.component;

import com.core.redis.RedisKey;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveHashCommands;
import org.springframework.data.redis.connection.ReactiveKeyCommands;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveStringCommands;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RedisReactiveOperationManager<K extends RedisKey> {

    @Autowired
    private ReactiveStringCommands reactiveStringCommands;
    @Autowired
    private ReactiveKeyCommands keyCommands;
    @Autowired
    private ReactiveHashCommands hashCommands;

    public Mono<Boolean> writeValues(Mono<List<Pair<String, String>>> keysValuesPair) {

        Mono<Map<ByteBuffer, ByteBuffer>> keysValueMap = keysValuesPair.map(pairs -> pairs.stream()
                .collect(Collectors.toMap(toBuffer(Pair::getKey), toBuffer(Pair::getValue))));

        return reactiveStringCommands.mSet(keysValueMap.map(ReactiveStringCommands.MSetCommand::mset)).next()
                .map(respone -> respone.getOutput());
    }

    public Function<Pair<String, String>, ByteBuffer> toBuffer(Function<Pair<String, String>, String> extractor) {

        return p -> ByteBuffer.wrap(extractor.apply(p).getBytes());
    }

    public Mono<Pair<List<K>, Mono<List<String>>>> readValues(Mono<List<K>> redisKey) {

        return Mono
                .just(new ImmutablePair(redisKey.block(),
                        reactiveStringCommands.mGet(redisKey.map(this::toListBuffer)).next()
                                .map(reponse -> {
                                    return toListString(reponse.getOutput());
                                })));
    }

    public Mono<Map<String, String>> read(Mono<String> redisKey) {

        Mono<ReactiveRedisConnection.KeyCommand> keyCommand = redisKey
                .map(r -> new ReactiveRedisConnection.KeyCommand(ByteBuffer.wrap(r.getBytes())));

        return (Mono) hashCommands.hGetAll(keyCommand).next().map(response -> response.getOutput()
                .map(e -> toStringMapEntry(e)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
    }

    public Flux<Map<String, String>> read(Flux<String> redisKeys) {

        return hashCommands
                .hGetAll(redisKeys.map(r -> new ReactiveRedisConnection.KeyCommand(ByteBuffer.wrap(r.getBytes()))))
                .map(e -> toMapString(e.getOutput()).block());
    }

    public Mono<Boolean> write(Mono<String> redisKey, Mono<Map<String, String>> attributes) {

        Mono<ReactiveHashCommands.HSetCommand> hSetCommand = Mono.zip(redisKey, attributes)
                .map(pair -> ReactiveHashCommands.HSetCommand.fieldValues(toByteBufferMap(pair.getT2()))
                        .forKey(ByteBuffer.wrap(pair.getT1().getBytes())));

        return hashCommands.hSet(hSetCommand).next().map(response -> response.getOutput());

    }

    public Flux<Boolean> write(Flux<Pair<String, Map<String, String>>> keyAttrs) {

        Flux<ReactiveHashCommands.HSetCommand> commands = keyAttrs
                .map(keyAttr -> ReactiveHashCommands.HSetCommand.fieldValues(toByteBufferMap(keyAttr.getValue()))
                        .forKey(ByteBuffer.wrap(keyAttr.getKey().getBytes())));

        return hashCommands.hSet(commands).map(r -> r.getOutput());

    }

    private Map<ByteBuffer, ByteBuffer> toByteBufferMap(Map<String, String> stringMap) {

        return stringMap.entrySet().stream()
                .map(e -> new DefaultMapEntry<ByteBuffer, ByteBuffer>(ByteBuffer.wrap(e.getKey().getBytes()),
                        ByteBuffer.wrap(e.getValue().getBytes())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

    }

    private List<String> toListString(List<ByteBuffer> byteBuffers) {

        return byteBuffers.stream().map(b -> StandardCharsets.UTF_8.decode(b).toString()).collect(Collectors.toList());
    }

    private List<ByteBuffer> toListBuffer(List<K> keys) {

        return keys.stream().map(K::getKey).map(String::getBytes).map(ByteBuffer::wrap).collect(Collectors.toList());
    }

    private String toString(ByteBuffer byteBuffer) {

        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    private Map.Entry<String, String> toStringMapEntry(Map.Entry<ByteBuffer, ByteBuffer> bufferEntry) {

        return Optional.of(bufferEntry)
                .map(e -> new DefaultMapEntry<String, String>(toString(e.getKey()), toString(e.getValue()))).get();
    }

    private Mono<Map<String, String>> toMapString(Flux<Map.Entry<ByteBuffer, ByteBuffer>> bufferEntries) {

        return bufferEntries.map(e -> toStringMapEntry(e))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

}
