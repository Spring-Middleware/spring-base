package com.core.mapper;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class User {

    private UUID uuid;
    private String fullname;
    private LocalDate birthDate;

}
