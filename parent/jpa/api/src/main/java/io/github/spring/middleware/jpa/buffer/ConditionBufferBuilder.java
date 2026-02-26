package io.github.spring.middleware.jpa.buffer;

import io.github.spring.middleware.jpa.search.Search;

import java.beans.BeanInfo;
import java.lang.reflect.Field;

public interface ConditionBufferBuilder<S extends Search> {

    ConditionBuffer build(S seach, Field field, BeanInfo beanInfo) throws Exception;


}



