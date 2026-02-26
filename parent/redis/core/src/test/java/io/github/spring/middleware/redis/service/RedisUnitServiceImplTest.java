package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.redis.data.ContainerInformation;
import io.github.spring.middleware.redis.data.ContainerInformationKey;
import io.github.spring.middleware.redis.data.UnitInformation;
import io.github.spring.middleware.redis.data.UnitInformationKey;
import io.github.spring.middleware.redis.service.unit.RedisUnitService;
import io.github.spring.middleware.redis.service.unit.RedisUnitServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class RedisUnitServiceImplTest extends CommonRedisServiceTestImpl<Map<String, String>> {

    @InjectMocks
    private RedisUnitService<ContainerInformationKey, UnitInformationKey, UnitInformation, ContainerInformation> redisUnitService = new RedisUnitServiceImpl();

    @Test
    public void testWriteAndUpdate() {

        Collection<ContainerInformation> containers = generateContainers(LocalDate.parse("2022-03-17"),
                LocalDate.parse("2022-03-30"), "TG", 8, null);

        redisUnitService.setRedisUnitContainers(containers, RedisFunctions.mappingUnitFunction(),
                RedisFunctions.mappingAttributeFunction(), RedisFunctions.mergerUnitFunction(),
                RedisFunctions.redisUnitContainerCreationFunction());

        Map<String, String> attributes = redisMap.get("2022-03-20");
        assertTrue(attributes.entrySet().stream()
                .allMatch(e -> {
                    UnitInformationKey key = JsonHelper.toUnitInformationKey(e.getKey());
                    int code = Integer.parseInt(key.getCode());
                    return code < 8 && key.getExternalSystem().equals("TG");
                }));

        containers = generateContainers(LocalDate.parse("2022-03-20"),
                LocalDate.parse("2022-03-20"), "TG", 4, "FIXED");

        redisUnitService.setRedisUnitContainers(containers, RedisFunctions.mappingUnitFunction(),
                RedisFunctions.mappingAttributeFunction(), RedisFunctions.mergerUnitFunction(),
                RedisFunctions.redisUnitContainerCreationFunction());

        attributes = redisMap.get("2022-03-20");
        assertEquals(attributes.entrySet().stream().filter(e -> {
            UnitInformationKey key = JsonHelper.toUnitInformationKey(e.getKey());
            int code = Integer.parseInt(key.getCode());
            return code < 4;
        }).count(), 4);

        assertTrue(attributes.entrySet().stream().filter(e -> {
            UnitInformationKey key = JsonHelper.toUnitInformationKey(e.getKey());
            int code = Integer.parseInt(key.getCode());
            return code < 4;
        }).allMatch(e -> {
            UnitInformation unitInformation = JsonHelper.toUnitInformation(e.getValue());
            return unitInformation.getText().equals("FIXED");
        }));
    }

    @Test
    public void testReadNoFilters() {

        Collection<ContainerInformation> containers = generateContainers(LocalDate.parse("2022-03-17"),
                LocalDate.parse("2022-03-30"), "TG", 8, null);

        redisUnitService.setRedisUnitContainers(containers, RedisFunctions.mappingUnitFunction(),
                RedisFunctions.mappingAttributeFunction(), RedisFunctions.mergerUnitFunction(),
                RedisFunctions.redisUnitContainerCreationFunction());

        Collection<ContainerInformationKey> keys = containers.stream().map(ContainerInformation::getRedisKey).collect(
                Collectors.toSet());

        Collection<ContainerInformation> readContainers = redisUnitService
                .getRedisUnitContainers(keys, RedisFunctions.mappingUnitFunction(),
                        RedisFunctions.redisUnitContainerCreationFunction(), null);

        assertTrue(readContainers.stream().allMatch(readContainer -> {
            return containers.stream().anyMatch(c -> matchContainers(readContainer, c));
        }));

    }

    @Test
    public void testReadWithFilters() {

        Collection<ContainerInformation> containers = generateContainers(LocalDate.parse("2022-03-17"),
                LocalDate.parse("2022-03-30"), "TG", 8, null);

        redisUnitService.setRedisUnitContainers(containers, RedisFunctions.mappingUnitFunction(),
                RedisFunctions.mappingAttributeFunction(), RedisFunctions.mergerUnitFunction(),
                RedisFunctions.redisUnitContainerCreationFunction());

        Collection<ContainerInformationKey> keys = containers.stream().map(ContainerInformation::getRedisKey).collect(
                Collectors.toSet());

        Collection<ContainerInformation> readContainers = redisUnitService
                .getRedisUnitContainers(keys, RedisFunctions.mappingUnitFunction(),
                        RedisFunctions.redisUnitContainerCreationFunction(),
                        u -> Integer.parseInt(u.getKey().getCode()) == 4);

        assertTrue(readContainers.stream().allMatch(c -> {
            boolean afterDate = !LocalDate.parse(c.getRedisKey().getKey()).isBefore(LocalDate.parse("2022-03-17"));
            boolean beforeDate = !LocalDate.parse(c.getRedisKey().getKey()).isAfter(LocalDate.parse("2022-03-30"));
            boolean onlyCode = c.getRedisUnits().stream().allMatch(u -> Integer.parseInt(u.getKey().getCode()) == 4);
            boolean onlyOneUnit = c.getRedisUnits().size() == 1;
            return afterDate && beforeDate && onlyCode && onlyOneUnit;
        }));

    }

    private boolean matchContainers(ContainerInformation source, ContainerInformation target) {

        EqualsBuilder areEquals = new EqualsBuilder();
        areEquals.append(Optional.ofNullable(source).map(ContainerInformation::getRedisKey)
                        .map(ContainerInformationKey::getKey).orElse(null),
                Optional.ofNullable(target).map(ContainerInformation::getRedisKey).map(ContainerInformationKey::getKey)
                        .orElse(null));
        return areEquals.isEquals() && source.getRedisUnits().stream()
                .allMatch(u -> target.getRedisUnits().stream().anyMatch(ur -> matchUnitInformation(u, ur)));
    }

    private boolean matchUnitInformation(UnitInformation source, UnitInformation target) {

        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(source.getKey(), target.getKey());
        equalsBuilder.append(source.getText(), target.getText());
        return equalsBuilder.isEquals();
    }

    private Collection<ContainerInformation> generateContainers(LocalDate dateFrom, LocalDate dateTo, String system,
                                                                int quantity, String text) {

        LocalDate date = dateFrom;
        Collection<ContainerInformation> containerInformations = new HashSet<>();
        while (!date.isAfter(dateTo)) {
            containerInformations.add(ContainerInformation.builder()
                    .informationKey(ContainerInformationKey.builder().key(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .build())
                    .unitInformations(generateUnitsInformation(quantity, system, text))
                    .build());
            date = date.plusDays(1);
        }
        return containerInformations;
    }

    private Collection<UnitInformation> generateUnitsInformation(int quantity, String system, String text) {

        return IntStream.range(0, quantity).mapToObj(i -> {
            return UnitInformation.builder()
                    .text(text == null ? UUID.randomUUID().toString() : text)
                    .key(UnitInformationKey.builder()
                            .externalSystem(system)
                            .code(String.valueOf(i))
                            .build()).build();
        }).collect(Collectors.toSet());
    }

}
