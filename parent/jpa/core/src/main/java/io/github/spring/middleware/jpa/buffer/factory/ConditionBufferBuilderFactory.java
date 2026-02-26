package io.github.spring.middleware.jpa.buffer.factory;

import io.github.spring.middleware.jpa.annotations.ConditionBufferBuilderClass;
import io.github.spring.middleware.jpa.buffer.ConditionBufferBuilder;
import io.github.spring.middleware.jpa.buffer.JoinBuffer;
import io.github.spring.middleware.jpa.buffer.builder.CommonConditionBufferBuilderImpl;
import io.github.spring.middleware.jpa.query.ParameterCounter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;

@Component
public class ConditionBufferBuilderFactory implements ApplicationContextAware {

    @Autowired
    private static ApplicationContext applicationContext;

    public static ConditionBufferBuilder getConditionBufferBuilder(Field field, JoinBuffer joinBuffer,
                                                                   ParameterCounter parameterCounter) throws Exception {

        ConditionBufferBuilderClass conditionBufferBuilderClass = Arrays.stream(field.getAnnotations())
                .filter(ann -> ann.annotationType().isAnnotationPresent(ConditionBufferBuilderClass.class))
                .map(ann -> ann.annotationType().getAnnotation(ConditionBufferBuilderClass.class)).findFirst()
                .orElse(null);

        CommonConditionBufferBuilderImpl commonConditionBufferBuilderImpl = null;
        if (conditionBufferBuilderClass != null) {
            if (applicationContext != null) {
                commonConditionBufferBuilderImpl = (CommonConditionBufferBuilderImpl) applicationContext
                        .getBean(conditionBufferBuilderClass.value());
                commonConditionBufferBuilderImpl.setJoinBuffer(joinBuffer);
                commonConditionBufferBuilderImpl.setParameterCounter(parameterCounter);
                return commonConditionBufferBuilderImpl;
            } else {
                throw new UnsupportedOperationException("Missing applicationCotext");
            }
        }
        return commonConditionBufferBuilderImpl;
    }

    @Override
    public void setApplicationContext(ApplicationContext myApplicationContext) throws BeansException {

        applicationContext = myApplicationContext;
    }
}
