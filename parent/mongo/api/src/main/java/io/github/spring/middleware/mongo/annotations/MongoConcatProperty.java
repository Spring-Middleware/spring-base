package io.github.spring.middleware.mongo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@MongoAddFieldProperty(resolverClazz = "io.github.spring.middleware.mongo.resolver.MongoConcatPropertyResolver")
public @interface MongoConcatProperty {

    String separator() default " ";

    String value();

    String[] concat();

    @Deprecated
    boolean isArray() default false;

}
