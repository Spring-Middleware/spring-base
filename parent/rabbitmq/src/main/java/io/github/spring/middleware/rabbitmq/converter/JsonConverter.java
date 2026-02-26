package io.github.spring.middleware.rabbitmq.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Stream;


public class JsonConverter<T> implements Converter<T> {

    private Class<T> clazz;
    private Class<?>[] generalizedType;
    private Logger logger = LoggerFactory.getLogger(JsonConverter.class);
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


    public T toObject(String body, Module... modules)
            throws ConverterException {
        T t = null;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setTimeZone(TimeZone.getDefault());
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
        return toString(t, null, true, modules);
    }

    public String toString(T t) throws ConverterException {
        return toString(t, null, true, null);
    }

    public String toStringWithoutJavaTime(T t) throws ConverterException {
        return toString(t, null, false, null);
    }


    public String toString(T t, DateFormat dateFormat, boolean registerJavTimeModule, Module... modules)
            throws ConverterException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.registerModule(new Jdk8Module());
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

    public String getMediaType() {
        return MediaType.APPLICATION_JSON.toString();
    }

}
