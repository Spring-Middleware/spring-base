package com.middleware.util;

import com.middleware.annotation.MappedParam;
import lombok.Data;

@Data
public class PriceTypeRequest {

    @MappedParam
    private Integer adult;

    @MappedParam
    private Integer children;

}
