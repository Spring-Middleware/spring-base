package io.github.spring.middleware.rabbitmq.converter;
import com.fasterxml.jackson.databind.Module;

public interface Converter<T> {

    T toObject(String body) throws ConverterException;

    T toObject(String body, Module... modules) throws ConverterException;

    String toString(T t) throws ConverterException;

    String getMediaType();

}
