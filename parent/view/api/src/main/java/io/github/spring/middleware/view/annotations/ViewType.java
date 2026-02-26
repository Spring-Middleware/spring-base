package io.github.spring.middleware.view.annotations;

import io.github.spring.middleware.view.View;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewType {

    Class<? extends View> clazzView();

    String clazzName();

}
