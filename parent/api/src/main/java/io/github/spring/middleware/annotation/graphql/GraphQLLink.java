package io.github.spring.middleware.annotation.graphql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLLink {

    String schema();

    String type();

    String query();

    GraphQLLinkArgument[] arguments();

    boolean collection() default false;

    boolean batched() default false;
}
