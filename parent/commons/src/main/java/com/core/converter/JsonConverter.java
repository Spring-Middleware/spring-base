package com.core.converter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.collections4.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Stream;


public class JsonConverter<T> {

    private Class<T> clazz;
    private Class<?>[] generalizedType;
    private Map<SerializationFeature, Boolean> serializationFeatures = new HashMap<>();

    public JsonConverter(Class<T> clazz, Class<?>... generalizedType) {

        this.clazz = clazz;
        this.generalizedType = generalizedType;
    }

    public T toObject(String body) throws ConverterException {

        return toObject(body, null);
    }

    public void setSerializationFeature(SerializationFeature serializationFeature, Boolean state) {

        this.serializationFeatures.put(serializationFeature, state);
    }

    public T toObject(String body, Module... modules) throws ConverterException {

        T t = null;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        try {
            if (clazz.isAssignableFrom(String.class)) {
                t = (T) body;
            } else {
                Optional.ofNullable(modules).ifPresent(mods -> Stream.of(mods).forEach(module -> {
                    objectMapper.registerModule(module);
                }));
                if (generalizedType != null) {
                    JavaType typeDeserialize = objectMapper.getTypeFactory()
                            .constructParametricType(clazz, generalizedType);
                    t = objectMapper.readValue(body, typeDeserialize);
                } else {
                    t = objectMapper.readValue(body, clazz);
                }
            }
        } catch (Exception ex) {
            throw new ConverterException(ex);
        }
        return t;
    }


    public String toString(T t, Module... modules) throws ConverterException {

        return toString(t, null, true, null, modules);
    }

    public String toString(T t) throws ConverterException {

        return toString(t, null, true, null, null);
    }

    public String toString(T t, Collection<MixInDefinition> mixInDefinitions) throws ConverterException {

        return toString(t, null, true, mixInDefinitions, null);
    }

    public String toStringWithoutJavaTime(T t) throws ConverterException {

        return toString(t, null, false, null, null);
    }

    public String toString(T t, DateFormat dateFormat, boolean registerJavTimeModule,
                           Collection<MixInDefinition> mixInDefinitions, Module... modules)
            throws ConverterException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.registerModule(new Jdk8Module());
        CollectionUtils.emptyIfNull(mixInDefinitions).stream().forEach(mixInDefinition -> {
            objectMapper.addMixIn(mixInDefinition.getTargetClazz(), mixInDefinition.getMixInClazz());
        });
        Optional.ofNullable(serializationFeatures).ifPresent(config -> config.entrySet().stream().forEach(entry -> {
            objectMapper.configure(entry.getKey(), entry.getValue());
        }));
        Optional.ofNullable(modules).ifPresent(mods -> Stream.of(mods).forEach(module -> {
            objectMapper.registerModule(module);
        }));
        if (registerJavTimeModule) {
            objectMapper.registerModule(new JavaTimeModule());
        }
        if (dateFormat != null) {
            objectMapper.getSerializationConfig().with(dateFormat);
        }
        try {
            objectMapper.writeValue(bos, t);
            return bos.toString();
        } catch (Exception ex) {
            throw new ConverterException(ex);
        }
    }

}
