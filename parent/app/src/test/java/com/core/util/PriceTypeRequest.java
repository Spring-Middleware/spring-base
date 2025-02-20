package com.core.util;

import com.core.annotation.MappedParam;
import lombok.Data;

@Data
public class PriceTypeRequest {

    @MappedParam
    private Integer adult;

    @MappedParam
    private Integer children;

}
