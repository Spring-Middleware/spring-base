package com.middleware.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.middleware.error.ErrorMessage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collection;

@Getter
@Setter
public abstract class BaseResponse {

    private String responseId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime;
    private String server;
    private Collection<ErrorMessage> errors;

}
