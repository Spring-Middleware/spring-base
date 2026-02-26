package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.redis.component.RedisCommands;
import io.github.spring.middleware.redis.component.RedisConnectionManager;
import io.github.spring.middleware.redis.component.RedisOperationManager;
import io.github.spring.middleware.redis.service.RedisLock;
import io.github.spring.middleware.redis.service.RedisLockFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Slf4j
public class CommonRedisServiceTestImpl<T> {

    @Mock
    private RedisOperationManager redisOperationManager;
    @Mock
    private RedisConnectionManager redisConnectionManager;
    @Mock
    private RedisCommands redisCommands;
    @Mock
    private RedisLock redisLock;
    @Mock
    private RedisLockFactory redisLockFactory;

    protected Map<String, T> redisMap = new HashMap<>();

    @Before
    public void setUp() {

        try {
            MockitoAnnotations.initMocks(this);
            when(redisOperationManager.read(any(RedisCommands.class), anyString())).thenAnswer(invocationOnMock -> {
                String key = invocationOnMock.getArgument(1);
                return redisMap.get(key);
            });

            when(redisOperationManager.readValue(any(RedisCommands.class), anyString()))
                    .thenAnswer(invocationOnMock -> {
                        String key = invocationOnMock.getArgument(1);
                        return redisMap.get(key);
                    });

            when(redisOperationManager.readValues(any(RedisCommands.class), anyList())).thenAnswer(invocationOnMock -> {
                List keys = invocationOnMock.getArgument(1);
                return keys.stream().map(k -> redisMap.get(k)).collect(Collectors.toList());
            });

            doAnswer(invocationOnMock -> {
                String key = invocationOnMock.getArgument(1);
                Map<String, String> attributes = invocationOnMock.getArgument(2);
                return redisMap.put(key, (T) attributes);
            }).when(redisOperationManager).write(any(RedisCommands.class), anyString(), any(Map.class));

            doAnswer(invocationOnMock -> {
                String key = invocationOnMock.getArgument(1);
                String value = invocationOnMock.getArgument(2);
                return redisMap.put(key, (T) value);
            }).when(redisOperationManager).writeValue(any(RedisCommands.class), anyString(), anyString());

            doAnswer(invocationOnMock -> {
                List<Pair<String, T>> pairList = invocationOnMock.getArgument(1);
                pairList.stream().forEach(p -> {
                    redisMap.put(p.getKey(), p.getValue());
                });
                return pairList;
            }).when(redisOperationManager).writeValues(any(RedisCommands.class), anyList());

            when(redisLockFactory.getRedisLock(any(RedisCommands.class), anyString())).thenReturn(redisLock);
            when(redisLock.acquire()).thenReturn(true);
            doNothing().when(redisLock).release();

            when(redisConnectionManager.getConnection()).thenReturn(redisCommands);
            doNothing().when(redisConnectionManager).closeConnection(any(RedisCommands.class));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
