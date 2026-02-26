package io.github.spring.middleware.view;


public interface PropertyRolesAllowedAuthorizer {

    void authorize(String[] roles) throws SecurityException;

}
