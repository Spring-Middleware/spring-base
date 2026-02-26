package io.github.spring.middleware.rabbitmq.converter;


import org.springframework.http.MediaType;

public class ConverterFactory {



    public static <T> Converter createConverter(String mediaType, Class<T> clazz, Class<?>... generalizaedType) throws ConverterException {
        if (mediaType.equals(MediaType.APPLICATION_JSON.toString())) {
            return new JsonConverter(clazz, generalizaedType);
        } else if (mediaType.equals(MediaType.APPLICATION_XML.toString())) {
            return new XmlConverter(clazz);
        } else {
            throw new ConverterException("Invalid MediaType");
        }
    }

}
