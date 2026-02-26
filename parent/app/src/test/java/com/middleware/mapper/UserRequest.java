package com.middleware.mapper;

import com.middleware.request.BaseRequest;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class UserRequest extends BaseRequest {

    private UUID uuid;
    private String name;
    private String surname;
    private LocalDate birthDate;

}
