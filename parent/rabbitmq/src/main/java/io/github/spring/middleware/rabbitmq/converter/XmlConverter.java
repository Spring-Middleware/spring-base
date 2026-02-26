package io.github.spring.middleware.rabbitmq.converter;

import com.fasterxml.jackson.databind.Module;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.http.MediaType;


import java.io.StringReader;
import java.io.StringWriter;

public class XmlConverter<T> implements Converter<T> {

    private JAXBContext jaxbContext;
    private Class<T> clazz;

    public XmlConverter(Class<T> clazz) throws ConverterException {
        this.clazz = clazz;
        try {
            this.jaxbContext = JAXBContext.newInstance(clazz);
        } catch (Exception ex) {
            throw new ConverterException(ex);
        }
    }

    public T toObject(String body, Module... modules) throws ConverterException {
        return toObject(body);
    }

    public T toObject(String body) throws ConverterException {
        T t;
        try {
            if (clazz.isAssignableFrom(String.class)) {
                t = (T) body;
            } else {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                t = (T) unmarshaller.unmarshal(new StringReader(body));
            }
        } catch (Exception ex) {
            throw new ConverterException(ex);
        }
        return t;
    }

    public String toString(T t) throws ConverterException {
        StringWriter stringWriter = new StringWriter();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(t, stringWriter);
            return stringWriter.toString();
        } catch (Exception ex) {
            throw new ConverterException(ex);
        }
    }

    public String getMediaType() {
        return MediaType.APPLICATION_XML.toString();
    }

}
