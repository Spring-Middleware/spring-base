package com.middleware.mapper;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserViewModel {

    private UUID uuid;

    public String getFullname(String name, String surname) {

        return name + " " + surname;
    }

}
