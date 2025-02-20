package com.core.view;


public interface PropertyRolesAllowedAuthorizer {

    void authorize(String[] roles) throws SecurityException;

}
