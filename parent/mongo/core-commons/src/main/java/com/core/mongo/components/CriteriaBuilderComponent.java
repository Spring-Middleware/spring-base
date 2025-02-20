package com.core.mongo.components;

import com.core.mongo.annotations.MongoSearchClass;
import com.core.mongo.annotations.MongoSearchProperties;
import com.core.mongo.annotations.MongoSearchProperty;
import com.core.mongo.function.FunctionsConditionType;
import com.core.mongo.processor.MongoAnnotationProcessor;
import com.core.mongo.processor.MongoAnnotationProcessorParameters;
import com.core.mongo.search.MongoSearch;
import com.core.mongo.types.ConditionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class CriteriaBuilderComponent {

    @Autowired
    @Qualifier("SEARCH_PROPERTY")
    private MongoAnnotationProcessor searchPropertyProcessor;
    @Autowired
    @Qualifier("SEARCH_CLASS")
    private MongoAnnotationProcessor searchClassProcessor;
    @Autowired
    @Qualifier("SEARCH_PROPERTIES")
    private MongoAnnotationProcessor searchPropertiesProcessor;

    public <S extends MongoSearch> Criteria buildCriteria(S mongoSearch, Criteria criteria,
                                                          String path) throws Exception {
        BeanInfo beanInfo = null;
        Class<? super MongoSearch> clazzSearch = (Class) mongoSearch.getClass();
        Set<Criteria> criteriasAnd = new HashSet<>();
        Set<Criteria> criteriasOr = new HashSet<>();
        while (clazzSearch != null) {
            beanInfo = Introspector.getBeanInfo(clazzSearch);
            for (Field field : clazzSearch.getDeclaredFields()) {
                MongoAnnotationProcessorParameters.MongoAnnotationProcessorParametersBuilder parametersBuilder = MongoAnnotationProcessorParameters
                        .builder()
                        .field(field)
                        .mongoSearch(mongoSearch)
                        .beanInfo(beanInfo)
                        .path(path)
                        .andColl(criteriasAnd)
                        .orColl(criteriasOr);
                if (field.isAnnotationPresent(MongoSearchProperty.class)) {
                    parametersBuilder.annotation(field.getAnnotation(MongoSearchProperty.class));
                    searchPropertyProcessor.processAnnotation(parametersBuilder.build());
                } else if (field.isAnnotationPresent(MongoSearchClass.class)) {
                    parametersBuilder.annotation(field.getAnnotation(MongoSearchClass.class));
                    searchClassProcessor.processAnnotation(parametersBuilder.build());
                } else if (field.isAnnotationPresent(MongoSearchProperties.class)) {
                    parametersBuilder.annotation(field.getAnnotation(MongoSearchProperties.class));
                    searchPropertiesProcessor.processAnnotation(parametersBuilder.build());
                }
            }
            clazzSearch = clazzSearch.getSuperclass();
        }
        FunctionsConditionType.applyCondition(ConditionType.OR, criteria, criteriasOr);
        FunctionsConditionType.applyCondition(ConditionType.AND, criteria, criteriasAnd);
        return criteria;
    }

    public <S extends MongoSearch> Mono<Criteria> buildCriteriaAsync(Mono<S> mongoSearch, Criteria criteria,
                                                                     String path) {
        return mongoSearch.flatMap(search -> {
            Criteria buildCriteria = null;
            try {
                buildCriteria = buildCriteria(search, criteria, path);
            } catch (Exception ex) {
                log.error("Error building criteria async ", ex);
            }
            return Mono.justOrEmpty(buildCriteria);
        }).switchIfEmpty(Mono.error(new Exception("Error building criteria async")));
    }

}
