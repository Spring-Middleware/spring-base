package io.github.spring.middleware.view.annotations;


import io.github.spring.middleware.view.DataAdaptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAdapter {

    Class<? extends DataAdaptor> value();
}
