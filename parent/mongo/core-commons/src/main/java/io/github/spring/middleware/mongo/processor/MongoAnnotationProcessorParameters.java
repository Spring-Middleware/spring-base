package io.github.spring.middleware.mongo.processor;

import io.github.spring.middleware.mongo.search.MongoSearch;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.query.Criteria;

import java.beans.BeanInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

@Getter
@Setter
public class MongoAnnotationProcessorParameters<A extends Annotation, S extends MongoSearch> {

    private A annotation;
    private Field field;
    private String path;
    private BeanInfo beanInfo;
    private S mongoSearch;
    private Collection<Criteria> andColl;
    private Collection<Criteria> orColl;

    @Builder
    public MongoAnnotationProcessorParameters(A annotation, Field field, String path, BeanInfo beanInfo, S mongoSearch,
                                              Collection<Criteria> andColl,
                                              Collection<Criteria> orColl) {

        this.annotation = annotation;
        this.field = field;
        this.path = path;
        this.beanInfo = beanInfo;
        this.mongoSearch = mongoSearch;
        this.andColl = andColl;
        this.orColl = orColl;
    }
}
