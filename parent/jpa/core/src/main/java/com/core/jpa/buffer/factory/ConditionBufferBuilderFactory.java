package com.core.jpa.buffer.factory;

import com.core.jpa.annotations.ConditionBufferBuilderClass;
import com.core.jpa.buffer.ConditionBufferBuilder;
import com.core.jpa.buffer.JoinBuffer;
import com.core.jpa.buffer.builder.CommonConditionBufferBuilderImpl;
import com.core.jpa.query.ParameterCounter;
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
