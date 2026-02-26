package io.github.spring.middleware.redis.service;

import io.github.spring.middleware.converter.JsonConverter;
import io.github.spring.middleware.redis.data.ContainerInformationKey;
import io.github.spring.middleware.redis.data.ContainerInformationValue;
import io.github.spring.middleware.redis.data.DataKeyValueRequest;
import io.github.spring.middleware.redis.data.DataValue;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RedisServiceImplTest extends CommonRedisServiceTestImpl<String> {

    @InjectMocks
    private RedisService<ContainerInformationKey, DataValue, ContainerInformationValue> redisService = new RedisServiceImpl<>();

    private JsonConverter<DataValue> dataValueJsonConverter = new JsonConverter<>(DataValue.class);

    @Test
    public void testSetKeyValues() {

        List<ContainerInformationKey> keys = generateContainerKeys(LocalDate.parse("2022-04-01"),
                LocalDate.parse("2022-04-15"));

        Collection<DataKeyValueRequest> dataKeyValueRequests = generateDataKeyValueRequest(
                LocalDate.parse("2022-04-01"),
                LocalDate.parse("2022-04-10"), "NAME_A", "VALUE_A");

        dataKeyValueRequests.addAll(generateDataKeyValueRequest(LocalDate.parse("2022-04-12"),
                LocalDate.parse("2022-04-15"), "NAME_B", "VALUE_B"));

        redisService
                .setKeyValues(keys, new ContainerSetFunctions(dataKeyValueRequests));

        String json = redisMap.get("2022-04-05");
        DataValue dataValue = dataValueJsonConverter.toObject(json);
        assertEquals(dataValue.getName(), "NAME_A");
        assertEquals(dataValue.getValue(), "VALUE_A");

        json = redisMap.get("2022-04-12");
        dataValue = dataValueJsonConverter.toObject(json);
        assertEquals(dataValue.getName(), "NAME_B");
        assertEquals(dataValue.getValue(), "VALUE_B");

        json = redisMap.get("2022-04-11");
        dataValue = dataValueJsonConverter.toObject(json);
        assertNull(dataValue.getName());
        assertNull(dataValue.getValue());
    }

    @Test
    public void testReadValues() {

        List<ContainerInformationKey> keys = generateContainerKeys(LocalDate.parse("2022-04-01"),
                LocalDate.parse("2022-04-15"));

        Collection<DataKeyValueRequest> dataKeyValueRequests = generateDataKeyValueRequest(
                LocalDate.parse("2022-04-01"),
                LocalDate.parse("2022-04-15"), "NAME", "VALUE");

        redisService
                .setKeyValues(keys, new ContainerSetFunctions(dataKeyValueRequests));

        Collection<ContainerInformationValue> informationValues = redisService
                .getKeyValues(keys, new ContainerGetFunctions());

        assertEquals(informationValues.size(), 15);
        assertTrue(informationValues.stream().allMatch(
                i -> !LocalDate.parse(i.getRedisKey().getKey(), DateTimeFormatter.ISO_LOCAL_DATE)
                        .isBefore(LocalDate.parse("2022-04-01")) &&
                        !LocalDate.parse(i.getRedisKey().getKey(), DateTimeFormatter.ISO_LOCAL_DATE)
                                .isAfter(LocalDate.parse("2022-04-15")) && i.getDataValue().getName().equals("NAME") &&
                        i.getDataValue().getValue().equals("VALUE")));

    }

    private List<ContainerInformationKey> generateContainerKeys(LocalDate dateFrom, LocalDate dateTo) {

        LocalDate date = dateFrom;
        List<ContainerInformationKey> containerInformationsKeys = new ArrayList<>();
        while (!date.isAfter(dateTo)) {
            containerInformationsKeys.add(ContainerInformationKey.builder()
                    .key(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).build());
            date = date.plusDays(1);
        }
        return containerInformationsKeys;
    }

    private Collection<DataKeyValueRequest> generateDataKeyValueRequest(LocalDate dateFrom, LocalDate dateTo,
                                                                        String name, String value) {

        LocalDate date = dateFrom;
        Collection<DataKeyValueRequest> dataKeyValueRequests = new HashSet<>();
        while (!date.isAfter(dateTo)) {
            dataKeyValueRequests.add(DataKeyValueRequest.builder()
                    .key(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .name(name)
                    .value(value).build());
            date = date.plusDays(1);
        }
        return dataKeyValueRequests;
    }

}
