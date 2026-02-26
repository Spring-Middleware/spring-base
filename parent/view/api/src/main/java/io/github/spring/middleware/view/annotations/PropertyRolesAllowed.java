package io.github.spring.middleware.view.annotations;

import io.github.spring.middleware.view.PropertyRolesAllowedAuthorizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyRolesAllowed {

    String[] value();

    Class<? extends PropertyRolesAllowedAuthorizer> authorizer() default AlwaysAllowed.class;

    class AlwaysAllowed implements PropertyRolesAllowedAuthorizer {

        public void authorize(String[] roles) throws SecurityException {
            //Do nothing
        }
    }

}
