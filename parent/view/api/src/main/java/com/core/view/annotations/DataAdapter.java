package com.core.view.annotations;


import com.core.view.DataAdaptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAdapter {

    Class<? extends DataAdaptor> value();
}
