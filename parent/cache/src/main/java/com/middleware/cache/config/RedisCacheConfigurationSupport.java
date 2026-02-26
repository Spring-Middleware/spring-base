package com.middleware.cache.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.middleware.cache.annotations.RedisCacheConfiguration;
import com.middleware.cache.data.RedisCacheConfigurationParameters;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@EnableCaching
@Configuration
public class RedisCacheConfigurationSupport implements EmbeddedValueResolverAware, CachingConfigurer {

    private StringValueResolver resolver;

    @Value("${spring.cache.redis.enable-statistics:false}")
    private boolean enableStadistics;

    private Collection<RedisCacheConfigurationParameters> redisCacheConfigurationParameters;

    @PostConstruct
    private void setUp() {

        redisCacheConfigurationParameters = getRedisCacheConfigurations();
        redisCacheConfigurationParameters.addAll(getSpecificRedisCacheConfigurations());
    }

    public Collection<RedisCacheConfigurationParameters> getRedisCacheConfigurationParameters() {

        return redisCacheConfigurationParameters;
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {

        return builder -> {
            redisCacheConfigurationParameters.stream().forEach(params -> {
                builder.withCacheConfiguration(params.getCacheName(),
                        org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(params.getTtl(), params.getChronoUnit()))
                                .disableKeyPrefix()
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                                        new GenericJackson2JsonRedisSerializer(getObjectMapper()))));

            });
            if (enableStadistics) {
                builder.enableStatistics();
            }
        };
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().failOnEmptyBeans(false)
                .failOnUnknownProperties(false)
                .indentOutput(false)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .modules(
                        // Optional
                        new Jdk8Module(),
                        // Dates/Times
                        new JavaTimeModule()
                ).build();
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }

    private Collection<RedisCacheConfigurationParameters> getRedisCacheConfigurations() {

        Reflections reflections = new Reflections("com", new MethodAnnotationsScanner());
        Set<Method> methods = reflections.getMethodsAnnotatedWith(Cacheable.class);
        return methods.stream().filter(m -> m.isAnnotationPresent(RedisCacheConfiguration.class)).map(m -> {
            Cacheable cacheable = m.getAnnotation(Cacheable.class);
            RedisCacheConfiguration redisCacheConfiguration = m.getAnnotation(RedisCacheConfiguration.class);

            return Arrays.stream(cacheable.value()).map(cacheName -> {
                return RedisCacheConfigurationParameters.builder()
                        .cacheName(cacheName)
                        .ttl(getTtl(redisCacheConfiguration))
                        .chronoUnit(getChronoUnit(redisCacheConfiguration)).build();
            }).collect(Collectors.toSet());

        }).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    private int getTtl(RedisCacheConfiguration redisCacheConfiguration) {

        if (!redisCacheConfiguration.ttlString().equals(StringUtils.EMPTY)) {
            return Integer.parseInt(resolver.resolveStringValue(redisCacheConfiguration.ttlString()));
        } else {
            return redisCacheConfiguration.ttl();
        }
    }

    private ChronoUnit getChronoUnit(RedisCacheConfiguration redisCacheConfiguration) {

        if (!redisCacheConfiguration.chronoUnitString().equals(StringUtils.EMPTY)) {
            return ChronoUnit.valueOf(resolver.resolveStringValue(redisCacheConfiguration.chronoUnitString()));
        } else {
            return redisCacheConfiguration.chronoUnit();
        }
    }

    protected Collection<RedisCacheConfigurationParameters> getSpecificRedisCacheConfigurations() {

        return new HashSet<>();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver stringValueResolver) {

        this.resolver = stringValueResolver;
    }
}
